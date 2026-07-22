/**
 *
 * (c) Copyright Ascensio System SIA 2026
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

import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.events.user.DocspaceLogoutUserEvent;
import com.onlyoffice.docspacepipedrive.manager.DocspaceOAuth2Manager;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.web.dto.docspaceaccount.DocspaceAccountResponse;
import com.onlyoffice.docspacepipedrive.web.dto.docspaceaccount.DocspaceOAuth2AuthorizeUrlResponse;
import com.onlyoffice.docspacepipedrive.web.dto.docspaceaccount.DocspaceOAuth2CallbackRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;


@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final DocspaceAccountService docspaceAccountService;
    private final DocspaceOAuth2Manager docspaceOAuth2Manager;
    private final ApplicationEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUser(@AuthenticationPrincipal OAuth2PipedriveUser currentUser) {
        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(
                currentUser.getClientId(),
                currentUser.getUserId()
        );

        Map<String, Object> userResponse = currentUser.getAttributes();
        userResponse.put("isAdmin", currentUser.getAuthorities().contains(
                new SimpleGrantedAuthority("DEAL_ADMIN")
        ));
        if (Objects.nonNull(docspaceAccount)) {
            userResponse.put("docspaceAccount", new DocspaceAccountResponse(
                    docspaceAccount.getEmail(),
                    Objects.nonNull(docspaceAccount.getAccessToken())
                            ? docspaceAccount.getAccessToken().getValue()
                            : null
            ));
        } else {
            userResponse.put("docspaceAccount", null);
        }


        return ResponseEntity.ok(
                userResponse
        );
    }

    @GetMapping("/docspace-account/oauth2/authorize-url")
    public ResponseEntity<DocspaceOAuth2AuthorizeUrlResponse> getDocspaceOAuth2AuthorizeUrl(
            @AuthenticationPrincipal OAuth2PipedriveUser currentUser) {
        String authorizeUrl = docspaceOAuth2Manager.buildAuthorizeUrl(
                currentUser.getClientId(),
                currentUser.getUserId()
        );

        return ResponseEntity.ok(new DocspaceOAuth2AuthorizeUrlResponse(authorizeUrl));
    }

    @PostMapping("/docspace-account/oauth2/callback")
    @Transactional
    public ResponseEntity<Void> postDocspaceOAuth2Callback(@AuthenticationPrincipal OAuth2PipedriveUser currentUser,
                                                            @Valid @RequestBody DocspaceOAuth2CallbackRequest request) {
        docspaceOAuth2Manager.handleCallback(
                currentUser.getClientId(),
                currentUser.getUserId(),
                request.getCode(),
                request.getState()
        );

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/docspace-account")
    public ResponseEntity<Void> deleteDocspaceAccount(@AuthenticationPrincipal OAuth2PipedriveUser currentUser) {
        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(
                currentUser.getClientId(),
                currentUser.getUserId()
        );

        if (Objects.nonNull(docspaceAccount)) {
            docspaceAccountService.deleteByClientIdAndUserId(currentUser.getClientId(), currentUser.getUserId());

            eventPublisher.publishEvent(new DocspaceLogoutUserEvent(
                    this, docspaceAccount
            ));
        }

        return ResponseEntity.noContent().build();
    }
}
