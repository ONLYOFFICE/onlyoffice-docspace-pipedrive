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

package com.onlyoffice.docspacepipedrive.client.docspace.impl;

import com.onlyoffice.docspacepipedrive.client.docspace.DocspaceClient;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceApiKey;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceAuthentication;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceCSPSettings;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceGroup;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceMembers;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceResponse;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoom;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomInvitationRequest;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceRoomType;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceUser;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceWebClientResponseException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RequiredArgsConstructor
public class DocspaceClientImpl implements DocspaceClient {
    private static final int PAGINATION_COUNT = 100;

    private final WebClient cleanWebClient;
    private final WebClient authorizedWebClient;

    public String authenticate(final String docspaceUrl, final String userName, final String passwordHash) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userName);
        map.put("passwordHash", passwordHash);

        return cleanWebClient.post()
                .uri(UriComponentsBuilder.fromUriString(docspaceUrl)
                        .path("/api/2.0/authentication")
                        .build()
                        .toUriString())
                .bodyValue(map)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceAuthentication>>() { })
                .map(DocspaceResponse<DocspaceAuthentication>::getResponse)
                .map(DocspaceAuthentication::getToken)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    public void logout(final String docspaceUrl, final String token) {
        cleanWebClient.post()
                .uri(UriComponentsBuilder.fromUriString(docspaceUrl)
                        .path("/api/2.0/authentication/logout")
                        .build()
                        .toUriString())
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .toBodilessEntity()
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    public DocspaceCSPSettings getCSPSettings() {
        return authorizedWebClient.get()
                .uri("/api/2.0/security/csp")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceCSPSettings>>() { })
                .map(DocspaceResponse<DocspaceCSPSettings>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    public DocspaceCSPSettings updateCSPSettings(final List<String> domains) {
        Map<String, Object> map = new HashMap<>();
        map.put("domains", domains);

        return authorizedWebClient.post()
                .uri("/api/2.0/security/csp")
                .bodyValue(map)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceCSPSettings>>() { })
                .map(DocspaceResponse<DocspaceCSPSettings>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    public List<DocspaceApiKey> getApiKeys() {
        return authorizedWebClient.get()
                .uri("api/2.0/keys")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<List<DocspaceApiKey>>>() { })
                .map(DocspaceResponse<List<DocspaceApiKey>>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    public DocspaceUser getUser() {
        return authorizedWebClient.get()
                .uri("/api/2.0/people/@self")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceUser>>() { })
                .map(DocspaceResponse<DocspaceUser>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public DocspaceUser getUser(final String email) {
        return authorizedWebClient.get()
                .uri(uriBuilder -> {
                    return uriBuilder.path("/api/2.0/people/email")
                            .queryParam("email", email)
                            .build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceUser>>() { })
                .map(DocspaceResponse<DocspaceUser>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public DocspaceUser getUser(final UUID id) {
        return authorizedWebClient.get()
                .uri(uriBuilder -> {
                    return uriBuilder.path("/api/2.0/people/{id}")
                            .build(id);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceUser>>() { })
                .map(DocspaceResponse<DocspaceUser>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public List<DocspaceUser> findUsers(final Integer employeeType) {
        List<DocspaceUser> docspaceUsers = new ArrayList<>();

        boolean moreItemInCollection = true;
        Integer startIndex = 0;
        Integer count = PAGINATION_COUNT;

        while (moreItemInCollection) {
             DocspaceResponse<List<DocspaceUser>> response = authorizedWebClient.get()
                    .uri(UriComponentsBuilder.fromUriString("")
                            .path("/api/2.0/people/simple/filter")
                            .queryParam("employeeType", employeeType)
                            .queryParam("startIndex", startIndex)
                            .queryParam("count", count)
                            .build()
                            .toUriString()
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<List<DocspaceUser>>>() {
                    })
                    .onErrorResume(WebClientResponseException.class, e -> {
                        return Mono.error(new DocspaceWebClientResponseException(e));
                    })
                    .block();

            docspaceUsers.addAll(response.getResponse());

            startIndex = startIndex + count;
            if (response.getTotal() <= startIndex) {
                moreItemInCollection = false;
            }
        }

        return docspaceUsers;
    }

    @Override
    public DocspaceRoom createRoom(final String title, final DocspaceRoomType roomType, final List<String> tags) {
        Map<String, Object> map = new HashMap<>();

        map.put("title", title);
        map.put("roomType", roomType);
        map.put("tags", tags);

        return authorizedWebClient.post()
                .uri("/api/2.0/files/rooms")
                .bodyValue(map)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceRoom>>() { })
                .map(DocspaceResponse<DocspaceRoom>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public DocspaceMembers shareRoom(final Long roomId,
                                     final DocspaceRoomInvitationRequest docspaceRoomInvitationRequest) {
        return authorizedWebClient.put()
                .uri(uriBuilder -> {
                    return uriBuilder.path("api/2.0/files/rooms/{roomId}/share")
                            .build(roomId);
                })
                .bodyValue(docspaceRoomInvitationRequest)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceMembers>>() { })
                .map(DocspaceResponse<DocspaceMembers>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public DocspaceGroup createGroup(final String name, final UUID owner, final List<UUID> members) {
        Map<String, Object> map = new HashMap<>();

        map.put("groupName", name);
        map.put("groupManager", owner);
        map.put("members", members);

        return authorizedWebClient.post()
                .uri("/api/2.0/group")
                .bodyValue(map)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceGroup>>() { })
                .map(DocspaceResponse<DocspaceGroup>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

    @Override
    public DocspaceGroup updateGroup(final UUID groupId, final String groupName, final UUID groupManager,
                                     final List<UUID> membersToAdd, final List<UUID> membersToRemove) {
        Map<String, Object> map = new HashMap<>();

        if (groupId != null) {
            map.put("groupName", groupName);
        }
        if (groupManager != null) {
            map.put("groupManager", groupManager);
        }
        if (membersToAdd != null) {
            map.put("membersToAdd", membersToAdd);
        }
        if (membersToRemove != null) {
            map.put("membersToRemove", membersToRemove);
        }

        return authorizedWebClient.put()
                .uri(uriBuilder -> {
                    return uriBuilder.path("/api/2.0/group/{groupId}")
                            .build(groupId);
                })
                .bodyValue(map)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceGroup>>() { })
                .map(DocspaceResponse<DocspaceGroup>::getResponse)
                .onErrorResume(WebClientResponseException.class, e -> {
                    return Mono.error(new DocspaceWebClientResponseException(e));
                })
                .block();
    }

}
