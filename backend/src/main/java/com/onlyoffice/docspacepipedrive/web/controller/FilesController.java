/**
 *
 * (c) Copyright Ascensio System SIA 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.onlyoffice.docspacepipedrive.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlyoffice.docspacepipedrive.client.docspace.DocspaceClient;
import com.onlyoffice.docspacepipedrive.client.pipedrive.PipedriveClient;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceAccountNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceOperationException;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import com.onlyoffice.docspacepipedrive.web.dto.files.SendFileRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FilesController {
    private final PipedriveClient pipedriveClient;
    private final DocspaceClient docspaceClient;
    private final SettingsService settingsService;
    private final DocspaceAccountService docspaceAccountService;

    @PostMapping("/send/from-docspace-to-pipedrive")
    public ResponseEntity<JsonNode> sendFromDocspaceToPipedrive(
            @AuthenticationPrincipal OAuth2PipedriveUser currentUser,
            @Valid @RequestBody SendFileRequest request
    ) {
        Settings settings = settingsService.findByClientId(currentUser.getClientId());
        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(
                currentUser.getClientId(),
                currentUser.getUserId()
        );
        if (docspaceAccount == null) {
            throw new DocspaceAccountNotFoundException(currentUser.getUserId(), currentUser.getClientId());
        }
        String docspaceUrl = settings.getUrl();
        String token = docspaceClient.authenticate(
                docspaceUrl,
                docspaceAccount.getEmail(),
                docspaceAccount.getPasswordHash()
        );

        try {
            var tuple = Mono.zip(
                    docspaceClient.getFile(request.getTargetId(), docspaceUrl, token),
                    docspaceClient.downloadFile(request.getTargetId(), docspaceUrl, token)
            ).block();

            if (tuple == null) {
                throw new DocspaceOperationException("send file to pipedrive", "empty response");
            }

            return pipedriveClient.uploadFile(
                            request.getDestinationId(),
                            tuple.getT1().getTitle(),
                            tuple.getT2().getBody()
                    )
                    .map(ResponseEntity::ok)
                    .switchIfEmpty(Mono.error(new DocspaceOperationException(
                            "send file to pipedrive",
                            "empty response"
                    )))
                    .block();
        } finally {
            try {
                docspaceClient.logout(docspaceUrl, token);
            } catch (RuntimeException e) {
                log.warn("Failed to logout from DocSpace after file transfer", e);
            }
        }
    }
}
