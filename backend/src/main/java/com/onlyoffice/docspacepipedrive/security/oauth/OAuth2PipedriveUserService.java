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

package com.onlyoffice.docspacepipedrive.security.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2PipedriveUserService extends DefaultOAuth2UserService {
    private final WebClient pipedriveWebClient;

    @Value("${spring.security.oauth2.client.provider.pipedrive.user-info-uri}")
    private String userInfoUri;

    @Override
    public OAuth2User loadUser(final OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
         OAuth2User user = super.loadUser(userRequest);

         return new OAuth2PipedriveUser((Map<String, Object>) user.getAttributes().get("data"));
    }

    public OAuth2User loadUser(final Long clientId, final Long userId) {
        Map<String, Object> response = pipedriveWebClient.get()
                .uri(userInfoUri)
                .attribute(Authentication.class.getName(), createAuthentication(clientId + ":" + userId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() { })
                .block();

        return new OAuth2PipedriveUser((Map<String, Object>) response.get("data"));
    }

    private static Authentication createAuthentication(final String principalName) {
        return new AbstractAuthenticationToken((Collection) null) {
            public Object getCredentials() {
                return "";
            }

            public Object getPrincipal() {
                return principalName;
            }
        };
    }
}
