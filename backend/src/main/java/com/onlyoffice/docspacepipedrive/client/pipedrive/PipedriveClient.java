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

package com.onlyoffice.docspacepipedrive.client.pipedrive;

import com.fasterxml.jackson.databind.JsonNode;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDeal;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDealFollower;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveDealFollowerEvent;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveFile;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveResponse;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveUser;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveUserSettings;
import com.onlyoffice.docspacepipedrive.client.pipedrive.dto.PipedriveWebhook;
import com.onlyoffice.docspacepipedrive.entity.Client;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveOAuth2AuthorizationException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveWebClientResponseException;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.security.util.SecurityUtils;
import com.onlyoffice.docspacepipedrive.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@RequiredArgsConstructor
public class PipedriveClient {
    private static final int PAGINATION_LIMIT = 100;

    @Value("${pipedrive.base-api-url}")
    private String baseApiUrl;

    private final ClientService clientService;
    private final WebClient pipedriveWebClient;

    public JsonNode getDeals(final Integer limit, final String cursor) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl())
                .path("/api/v2/deals");

        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        if (cursor != null) {
            builder.queryParam("cursor", cursor);
        }

        return pipedriveWebClient.get()
                .uri(builder.build().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public PipedriveDeal getDeal(final Long id) {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/deals/{id}")
                        .build(id)
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveDeal>>() { })
                .map(PipedriveResponse<PipedriveDeal>::getData)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .block();
    }

    public JsonNode searchDeals(final String term, final Integer limit, final String cursor) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl())
                .path("/api/v2/deals/search");

        if (term != null) {
            builder.queryParam("term", term);
        }

        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        if (cursor != null) {
            builder.queryParam("cursor", cursor);
        }

        return pipedriveWebClient.get()
                .uri(builder.build().toUri())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public List<PipedriveDealFollower> getDealFollowers(final Long id) {
        List<PipedriveDealFollower> dealFollowers = new ArrayList<>();

        boolean moreItemInCollection = true;
        Integer start = 0;
        Integer limit = PAGINATION_LIMIT;

        while (moreItemInCollection) {
            PipedriveResponse<List<PipedriveDealFollower>> response = pipedriveWebClient.get()
                    .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                            .path("/v1/deals/{id}/followers")
                            .queryParam("start", start)
                            .queryParam("limit", limit)
                            .build(id)
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<List<PipedriveDealFollower>>>() { })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        return Mono.error(new PipedriveWebClientResponseException(e));
                    })
                    .block();

            dealFollowers.addAll(Optional.ofNullable(response.getData()).orElse(List.of()));

            moreItemInCollection = response.getAdditionalData().getPagination().getMoreItemsInCollection();
            if (moreItemInCollection) {
                start = response.getAdditionalData().getPagination().getNextStart();
            }
        }

        return dealFollowers;
    }

    public List<PipedriveDealFollowerEvent> getDealFollowersFlow(final Long id) {
        List<PipedriveDealFollowerEvent> followers = new ArrayList<>();

        boolean moreItemInCollection = true;
        Integer start = 0;
        Integer limit = PAGINATION_LIMIT;

        while (moreItemInCollection) {
            PipedriveResponse<List<PipedriveDealFollowerEvent>> response = pipedriveWebClient.get()
                    .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                            .path("/v1/deals/{id}/flow")
                            .queryParam("start", start)
                            .queryParam("limit", limit)
                            .queryParam("items", "dealFollower")
                            .build(id)
                    )
                    .retrieve()
                    .bodyToMono(
                            new ParameterizedTypeReference<PipedriveResponse<List<PipedriveDealFollowerEvent>>>() { }
                    )
                    .onErrorResume(WebClientResponseException.class, e -> {
                        return Mono.error(new PipedriveWebClientResponseException(e));
                    })
                    .block();

            followers.addAll(response.getData());

            moreItemInCollection = response.getAdditionalData().getPagination().getMoreItemsInCollection();
            if (moreItemInCollection) {
                start = response.getAdditionalData().getPagination().getNextStart();
            }
        }

        return followers;
    }

    public JsonNode getDealFiles(final Long dealId, final Integer start,
                                                               final Integer limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(getBaseUrl())
                .path("/v1/deals/{dealId}/files");

        if (start != null) {
            builder.queryParam("start", start);
        }

        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        return pipedriveWebClient.get()
                .uri(builder.build(dealId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public PipedriveUser getUser() {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/users/me")
                        .build()
                        .toUri()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveUser>>() { })
                .map(PipedriveResponse<PipedriveUser>::getData)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public List<PipedriveUser> getUsers() {
        List<PipedriveUser> users = new ArrayList<>();

        boolean moreItemInCollection = true;
        Integer start = 0;
        Integer limit = PAGINATION_LIMIT;

        while (moreItemInCollection) {
            PipedriveResponse<List<PipedriveUser>> response = pipedriveWebClient.get()
                    .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                            .path("/v1/users")
                            .queryParam("start", start)
                            .queryParam("limit", limit)
                            .build()
                            .toUri()
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<List<PipedriveUser>>>() { })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        return Mono.error(new PipedriveWebClientResponseException(e));
                    })
                    .block();

            users.addAll(response.getData());

            try {
                moreItemInCollection = response.getAdditionalData()
                        .getPagination()
                        .getMoreItemsInCollection();
            } catch (NullPointerException e) {
                moreItemInCollection = false;
            }

            if (moreItemInCollection) {
                start = response.getAdditionalData()
                        .getPagination()
                        .getNextStart();
            }
        }

        return users;
    }

    public PipedriveUserSettings getUserSettings() {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/userSettings")
                        .build()
                        .toUri()
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveUserSettings>>() { })
                .map(PipedriveResponse<PipedriveUserSettings>::getData)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public PipedriveWebhook createWebhook(final PipedriveWebhook pipedriveWebhook) {
        return pipedriveWebClient.post()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/webhooks")
                        .build()
                        .toUri()
                )
                .bodyValue(pipedriveWebhook)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveWebhook>>() { })
                .map(PipedriveResponse<PipedriveWebhook>::getData)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public void deleteWebhook(final Long id) {
        pipedriveWebClient.delete()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/webhooks/{id}")
                        .build(id)
                )
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                })
                .block();
    }

    public Mono<PipedriveFile> getFile(final Long fileId) {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/files/{fileId}")
                        .build(fileId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<PipedriveResponse<PipedriveFile>>() { })
                .map(PipedriveResponse<PipedriveFile>::getData)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                });
    }

    public Mono<ResponseEntity<Flux<DataBuffer>>> downloadFile(final Long fileId) {
        return pipedriveWebClient.get()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/files/{fileId}/download")
                        .build(fileId))
                .retrieve()
                .toEntityFlux(DataBuffer.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                });
    }

    public Mono<JsonNode> uploadFile(final Long dealId, final String fileName, final Flux<DataBuffer> file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.asyncPart("file", file, DataBuffer.class)
                .filename(fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        builder.part("deal_id", dealId);

        return pipedriveWebClient.post()
                .uri(UriComponentsBuilder.fromUriString(getBaseUrl())
                        .path("/v1/files")
                        .build()
                        .toUri()
                )
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new PipedriveWebClientResponseException(e));
                })
                .onErrorResume(OAuth2AuthorizationException.class, e -> {
                    return Mono.error(new PipedriveOAuth2AuthorizationException(e));
                });
    }

    private String getBaseUrl() {
        OAuth2PipedriveUser currentUser = SecurityUtils.getCurrentUser();
        Client client = clientService.findById(currentUser.getClientId());

        if (StringUtils.hasText(client.getUrl())) {
            return client.getUrl();
        }

        return baseApiUrl;
    }
}
