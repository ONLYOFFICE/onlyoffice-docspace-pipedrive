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

package com.onlyoffice.docspacepipedrive.manager;

import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceApiKey;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceResponse;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceUser;
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.exceptions.SettingsValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class DocspaceSettingsValidator {
    private static final List<String> REQUIRED_SCOPES = Arrays.asList(
            "accounts.self:read",
            "accounts:write",
            "rooms:write"
    );

    public Settings validate(final Settings settings) {
        String url = settings.getUrl();
        ApiKey apiKey = settings.getApiKey();

        WebClient webClient = WebClient.builder()
                .baseUrl(url)
                .defaultHeaders(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setBearerAuth(apiKey.getValue());
                })
                .build();

        checkApiKey(webClient);
        UUID apiKeyOwnerId = checkApiKeyOwner(webClient);

        apiKey.setOwnerId(apiKeyOwnerId);
        apiKey.setValid(true);
        return settings;
    }

    public void checkApiKey(final WebClient webClient) {
        DocspaceApiKey docspaceApiKey = getApiKey(webClient);

        if (Objects.isNull(docspaceApiKey)) {
            throw new SettingsValidationException(SettingsValidationException.ErrorCode.DOCSPACE_API_KEY_IS_INVALID);
        }

        List<String> scopes = docspaceApiKey.getPermissions();

        if (scopes.isEmpty()) {
            return;
        }

        List<String> missingScopes = new ArrayList<>();
        for (String requiredScope: REQUIRED_SCOPES) {
            if (!scopes.contains(requiredScope)) {
                missingScopes.add(requiredScope);
            }
        }

        if (!missingScopes.isEmpty()) {
            throw new SettingsValidationException(
                    SettingsValidationException.ErrorCode.DOCSPACE_API_KEY_IS_NOT_CONTAINS_REQUIRED_SCOPES,
                    Map.of("missingScopes", missingScopes)
            );
        }
    }

    public UUID checkApiKeyOwner(final WebClient webClient) {
        DocspaceUser docspaceUser = getUser(webClient);
        if (!docspaceUser.getIsOwner() && !docspaceUser.getIsAdmin()) {
            throw new SettingsValidationException(
                    SettingsValidationException.ErrorCode.DOCSPACE_API_KEY_OWNER_IS_NOT_ADMIN
            );
        }

        return docspaceUser.getId();
    }

    private DocspaceApiKey getApiKey(final WebClient webClient) {
        try {
            return webClient.get()
                    .uri("api/2.0/keys/@self")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceApiKey>>() { })
                    .map(DocspaceResponse<DocspaceApiKey>::getResponse)
                    .block();
        } catch (WebClientRequestException e) {
            log.warn("Error while getting DocSpace API keys", e);

            throw new SettingsValidationException(SettingsValidationException.ErrorCode.DOCSPACE_CAN_NOT_BE_REACHED);
        } catch (WebClientResponseException e) {
            log.warn("Error while getting DocSpace API keys", e);

            throw new SettingsValidationException(SettingsValidationException.ErrorCode.DOCSPACE_API_KEY_IS_INVALID);
        }
    }

    private DocspaceUser getUser(final WebClient webClient) {
        try {
            return webClient.get()
                    .uri("/api/2.0/people/@self")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<DocspaceResponse<DocspaceUser>>() { })
                    .map(DocspaceResponse<DocspaceUser>::getResponse)
                    .block();
        } catch (WebClientRequestException e) {
            log.warn("Error while getting DocSpace API key owner", e);

            throw new SettingsValidationException(SettingsValidationException.ErrorCode.DOCSPACE_CAN_NOT_BE_REACHED);
        } catch (WebClientResponseException e) {
            log.warn("Error while getting DocSpace API key owner", e);

            throw new SettingsValidationException(
                    SettingsValidationException.ErrorCode.DOCSPACE_API_KEY_OWNER_IS_NOT_ADMIN
            );
        }
    }

}
