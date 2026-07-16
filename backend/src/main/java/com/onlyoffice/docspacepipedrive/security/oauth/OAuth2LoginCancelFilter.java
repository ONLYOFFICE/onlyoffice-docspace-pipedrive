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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@RequiredArgsConstructor
public class OAuth2LoginCancelFilter extends OncePerRequestFilter {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RequestMatcher requestMatcher =
            new AntPathRequestMatcher("/login/oauth2/code/*", "GET");

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("pipedrive");

        URI redirectUrl = UriComponentsBuilder.fromUriString("https://www.pipedrive.com")
                .path("/marketplace/app/onlyoffice-doc-space/{clientId}")
                .build(clientRegistration.getClientId());

        response.sendRedirect(redirectUrl.toString());
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) throws ServletException {
        String error = request.getParameter("error");

        return !requestMatcher.matches(request) || !"user_denied".equals(error);
    }


}
