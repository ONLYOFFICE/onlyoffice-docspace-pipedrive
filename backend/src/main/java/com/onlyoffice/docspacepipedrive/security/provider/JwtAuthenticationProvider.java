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

package com.onlyoffice.docspacepipedrive.security.provider;

import com.onlyoffice.docspacepipedrive.security.RedisAuthenticationRepository;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveAuthenticationToken;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {
    private final OAuth2PipedriveUserService oAuth2PipedriveUserService;
    private final JwtDecoder jwtDecoder;
    private final RedisAuthenticationRepository redisAuthenticationRepository;

    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Value("${spring.security.jwt.client-name-attribute}")
    private String clientNameAttribute;
    @Value("${spring.security.jwt.user-name-attribute}")
    private String userNameAttribute;


    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(BearerTokenAuthenticationToken.class, authentication, () -> {
            return this.messages.getMessage(
                    "JwtAuthenticationProvider.onlySupports",
                    "Only BearerTokenAuthenticationToken is supported"
            );
        });

        BearerTokenAuthenticationToken bearer = (BearerTokenAuthenticationToken) authentication;

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(bearer.getToken());
        } catch (Exception e) {
            log.debug("Failed to authenticate since the JWT was invalid");
            throw new InvalidBearerTokenException(e.getMessage(), e);
        }

        Map<String, Object> body = jwt.getClaims();
        Long clientId = (Long) body.get(clientNameAttribute);
        Long userId = (Long) body.get(userNameAttribute);

        OAuth2AuthenticationToken resultAuthentication = (OAuth2AuthenticationToken)
                redisAuthenticationRepository.getAuthentication(clientId + ":" + userId, bearer.getToken());

        if (Objects.isNull(resultAuthentication)) {
            OAuth2User oAuth2User;
            try {
                oAuth2User = oAuth2PipedriveUserService.loadUser(clientId, userId);
            } catch (RuntimeException e) {
                throw new InvalidBearerTokenException(e.getMessage(), e);
            }

            resultAuthentication = createSuccessAuthentication(oAuth2User, bearer.getToken(), authentication);
            redisAuthenticationRepository.saveAuthentication(
                    clientId + ":" + userId,
                    resultAuthentication,
                    jwt.getExpiresAt()
            );
        }

        return resultAuthentication;
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return BearerTokenAuthenticationToken.class.isAssignableFrom(authentication);
    }

    protected OAuth2PipedriveAuthenticationToken createSuccessAuthentication(final OAuth2User oAuth2User,
                                                                    final String credentials,
                                                                    final Authentication authentication) {
        OAuth2PipedriveAuthenticationToken result = new OAuth2PipedriveAuthenticationToken(
                oAuth2User,
                oAuth2User.getAuthorities(),
                credentials,
                "pipedrive"
        );

        result.setDetails(authentication.getDetails());
        return result;
    }
}
