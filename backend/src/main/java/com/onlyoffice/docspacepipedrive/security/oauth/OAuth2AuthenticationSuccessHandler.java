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

import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.manager.PipedriveActionManager;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final PipedriveActionManager pipedriveActionManager;
    private final SettingsService settingsService;


    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws IOException, ServletException {
        OAuth2PipedriveUser oAuth2PipedriveUser = (OAuth2PipedriveUser) authentication.getPrincipal();

        String domain = oAuth2PipedriveUser.getAttribute("company_domain");

        if (isInitializeWebhooks(oAuth2PipedriveUser.getClientId())
                && oAuth2PipedriveUser.getAuthorities().contains(new SimpleGrantedAuthority("DEAL_ADMIN"))
        ) {
            pipedriveActionManager.initWebhooks();
        }

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("pipedrive");

        URI redirectUrl = UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(domain + ".pipedrive.com")
                .path("/settings/marketplace/app/{clientId}/app-settings")
                .build(clientRegistration.getClientId());

        redirectStrategy.sendRedirect(request, response, redirectUrl.toString());
    }

    private boolean isInitializeWebhooks(final Long clientId) {
        Settings settings = settingsService.findByClientId(clientId);

        String url = settings.getUrl();
        ApiKey apiKey = settings.getApiKey();

        return Objects.nonNull(url) && !url.isEmpty()
                && Objects.nonNull(apiKey) && apiKey.isValid()
                && !pipedriveActionManager.isWebhooksInstalled(clientId);
    }
}
