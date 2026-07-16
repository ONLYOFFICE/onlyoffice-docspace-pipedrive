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

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;

public class OAuth2PipedriveAuthenticationToken extends OAuth2AuthenticationToken {
    private final String credentials;

    public OAuth2PipedriveAuthenticationToken(final OAuth2User principal,
                                              final Collection<? extends GrantedAuthority> authorities,
                                              final String credentials, final String authorizedClientRegistrationId) {
        super(principal, authorities, authorizedClientRegistrationId);

        this.credentials = credentials;
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }
}
