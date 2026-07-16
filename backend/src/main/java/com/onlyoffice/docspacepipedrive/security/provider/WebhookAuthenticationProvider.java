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

import com.onlyoffice.docspacepipedrive.entity.Webhook;
import com.onlyoffice.docspacepipedrive.exceptions.WebhookNotFoundException;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.UUID;


@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookAuthenticationProvider implements AuthenticationProvider {
    private final WebhookService webhookService;
    private final PasswordEncoder passwordEncoder;
    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication, () -> {
            return this.messages.getMessage(
                    "WebhookAuthenticationProvider.onlySupports",
                    "Only UsernamePasswordAuthenticationToken is supported"
            );
        });

        Webhook webhook;
        try {
            webhook = webhookService.findById(UUID.fromString((String) authentication.getPrincipal()));
        } catch (WebhookNotFoundException e) {
            throw new BadCredentialsException(e.getMessage());
        }

        if (!passwordEncoder.matches(authentication.getCredentials().toString(), webhook.getPassword())) {
            throw new BadCredentialsException(
                    this.messages.getMessage(
                            "WebhookAuthenticationProvider.badCredentials",
                            "Bad credentials"
                    )
            );
        }

        return createSuccessAuthentication(
                webhook.getUser().getUserId(),
                webhook.getUser().getClient().getId(),
                authentication
        );
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    protected Authentication createSuccessAuthentication(final Long userId, final Long clientId,
                                                         final Authentication authentication) {
        OAuth2AuthenticationToken result = new OAuth2AuthenticationToken(
                new OAuth2PipedriveUser(
                        Map.of(
                                "id", userId,
                                "company_id", clientId
                        )
                ),
                List.of(),
                "pipedrive"
        );

        result.setDetails(authentication.getDetails());
        return result;
    }
}
