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

import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OAuth2PipedriveUser implements OAuth2User, Serializable {
    private static final String USER_ID_ATTRIBUTE_KEY = "id";
    private static final String CLIENT_ID_ATTRIBUTE_KEY = "company_id";
    private static final String AUTHORITIES_ATTRIBUTE_KEY = "access";
    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Object sourceAuthorities = attributes.get(AUTHORITIES_ATTRIBUTE_KEY);
        if (Objects.isNull(sourceAuthorities)) {
            return List.of();
        }

        List<Map<String, Object>> sourceAuthoritiesList = (List<Map<String, Object>>) sourceAuthorities;

        return sourceAuthoritiesList.stream()
                .map(sourceAuthority -> {
                    String app = (String) sourceAuthority.get("app");
                    boolean isAdmin = Boolean.TRUE.equals(sourceAuthority.get("admin"));

                    return switch (app) {
                        case "sales" -> new SimpleGrantedAuthority(isAdmin ? "DEAL_ADMIN" : "DEAL_REGULAR_USER");
                        case "global" -> new SimpleGrantedAuthority(isAdmin ? "GLOBAL_ADMIN" : "GLOBAL_REGULAR_USER");
                        default -> null;
                    };
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return getClientId() + ":" + getUserId();
    }

    public Long getUserId() {
        return ((Number) attributes.get(USER_ID_ATTRIBUTE_KEY)).longValue();
    }

    public Long getClientId() {
        return ((Number) attributes.get(CLIENT_ID_ATTRIBUTE_KEY)).longValue();
    }
}
