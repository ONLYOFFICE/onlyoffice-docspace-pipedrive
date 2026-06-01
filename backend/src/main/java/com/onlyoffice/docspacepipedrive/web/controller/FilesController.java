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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyoffice.docspacepipedrive.client.docspace.DocspaceClient;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspacePayload;
import com.onlyoffice.docspacepipedrive.client.pipedrive.PipedriveClient;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveFile;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FilesController {
    private static final int DOCSPACE_MAX_CHUNK_SIZE = 10 * 1024 * 1024;

    private final PipedriveClient pipedriveClient;
    private final DocspaceClient docspaceClient;
    private final SettingsService settingsService;
    private final DocspaceAccountService docspaceAccountService;
    private final ObjectMapper objectMapper;

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

    @PostMapping("/send/from-pipedrive-to-docspace")
    public ResponseEntity<JsonNode> sendFromPipedriveToDocspace(
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
                    pipedriveClient.getFile(request.getTargetId()),
                    pipedriveClient.downloadFile(request.getTargetId())
            ).block();

            if (tuple == null) {
                throw new DocspaceOperationException("send file to docspace", "empty response");
            }

            PipedriveFile pipedriveFile = tuple.getT1();
            long contentLength = tuple.getT2().getHeaders().getContentLength();
            long fileSize = contentLength < 0 ? pipedriveFile.getFileSize() : contentLength;
            Flux<DataBuffer> fileBody = tuple.getT2().getBody();

            return docspaceClient.createFileUploadSession(
                            request.getDestinationId(),
                            pipedriveFile.getName(),
                            fileSize,
                            docspaceUrl,
                            token
                    )
                    .flatMap(payload -> requireSuccessfulDocspacePayload(
                            payload,
                            "create upload session"
                    ))
                    .flatMap(session ->
                            splitStrictlyBySize(
                                    fileBody,
                                    DOCSPACE_MAX_CHUNK_SIZE
                            )
                                    .switchIfEmpty(Flux.just(new byte[0]))
                                    .concatMap(chunk ->
                                            docspaceClient.uploadFileChunk(
                                                    session.getId(),
                                                    chunk,
                                                    docspaceUrl,
                                                    token
                                            )
                                    )
                                    .concatMap(payload ->
                                            requireSuccessfulDocspacePayload(
                                                    payload,
                                                    "upload file chunk"
                                            )
                                    )
                                    .last()
                                    .map(ResponseEntity::ok)
                    )
                    .switchIfEmpty(Mono.error(new DocspaceOperationException(
                            "send file to docspace",
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



    private Flux<byte[]> splitStrictlyBySize(
            Flux<DataBuffer> source,
            int maxChunkSize
    ) {
        return Flux.create(sink -> {
            byte[] chunk = new byte[maxChunkSize];
            AtomicInteger position = new AtomicInteger(0);

            Disposable subscription = source.subscribe(dataBuffer -> {
                try {
                    int readable = dataBuffer.readableByteCount();

                    while (readable > 0) {
                        int currentPosition = position.get();
                        int freeSpace = maxChunkSize - currentPosition;
                        int bytesToRead = Math.min(freeSpace, readable);

                        dataBuffer.read(chunk, currentPosition, bytesToRead);

                        int newPosition = position.addAndGet(bytesToRead);
                        readable -= bytesToRead;

                        if (newPosition == maxChunkSize) {
                            sink.next(Arrays.copyOf(chunk, maxChunkSize));
                            position.set(0);
                        }
                    }
                } catch (Throwable e) {
                    sink.error(e);
                } finally {
                    DataBufferUtils.release(dataBuffer);
                }
            }, sink::error, () -> {
                int remaining = position.get();

                if (remaining > 0) {
                    sink.next(Arrays.copyOf(chunk, remaining));
                }

                sink.complete();
            });

            sink.onCancel(subscription::dispose);
            sink.onDispose(subscription::dispose);
        });
    }

    private <T> Mono<T> requireSuccessfulDocspacePayload(
            DocspacePayload<T> payload,
            String operation
    ) {
        if (payload == null || !Boolean.TRUE.equals(payload.getSuccess()) || payload.getData() == null) {
            String docspaceMessage = payload == null ? null : payload.getMessage();

            return Mono.error(new DocspaceOperationException(operation, docspaceMessage));
        }

        return Mono.just(payload.getData());
    }
}
