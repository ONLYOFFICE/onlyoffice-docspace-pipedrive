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

import com.onlyoffice.docspacepipedrive.security.token.ClientRegistrationAuthenticationToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;


@Component
@RequiredArgsConstructor
@Slf4j
public class ClientRegistrationAuthenticationProvider implements AuthenticationProvider {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication, () -> {
            return this.messages.getMessage(
                    "OAuth2ClientCredentialsAuthenticationProvider.onlySupports",
                    "Only UsernamePasswordAuthenticationToken is supported"
            );
        });

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("pipedrive");

        if (clientRegistration == null) {
            throw new BadCredentialsException(
                    this.messages.getMessage(
                            "OAuth2ClientCredentialsAuthenticationProvider.badCredentials",
                            "Bad credentials"
                    )
            );
        }

        if (!clientRegistration.getClientId().equals(authentication.getPrincipal())
                || !clientRegistration.getClientSecret().equals(authentication.getCredentials())) {
            throw new BadCredentialsException(
                    this.messages.getMessage(
                            "OAuth2ClientCredentialsAuthenticationProvider.badCredentials",
                            "Bad credentials"
                    )
            );
        }

        return createSuccessAuthentication(clientRegistration, authentication);
    }

    @Override
    public boolean supports(final Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    protected Authentication createSuccessAuthentication(final ClientRegistration clientRegistration,
                                                         final Authentication authentication) {
        ClientRegistrationAuthenticationToken result = new ClientRegistrationAuthenticationToken(clientRegistration);
        result.setDetails(authentication.getDetails());

        log.debug("Authenticated clientRegistration");
        return result;
    }
}
