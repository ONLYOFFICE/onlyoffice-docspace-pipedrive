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

package com.onlyoffice.docspacepipedrive.client.docspace.filter;

import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.settings.ApiKey;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyInvalidException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceUrlNotFoundException;
import com.onlyoffice.docspacepipedrive.security.oauth.OAuth2PipedriveUser;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

public class DocspaceAuthorizationApiKeyExchangeFilterFunction implements ExchangeFilterFunction {
    static final String SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY =
            "org.springframework.security.SECURITY_CONTEXT_ATTRIBUTES";
    private static final String AUTHENTICATION_ATTR_NAME = Authentication.class.getName();

    private final SettingsService settingsService;

    public DocspaceAuthorizationApiKeyExchangeFilterFunction(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public Mono<ClientResponse> filter(final ClientRequest request, final ExchangeFunction next) {
        return mergeRequestAttributesIfNecessary(request)
                .map(this::bearer)
                .flatMap(requestWithBearer -> exchangeAndHandleResponse(requestWithBearer, next));
    }

    private Mono<ClientResponse> exchangeAndHandleResponse(final ClientRequest request, final ExchangeFunction next) {
        return next.exchange(request)
                .flatMap(clientResponse -> {
                    HttpStatusCode statusCode = clientResponse.statusCode();
                    if (HttpStatus.UNAUTHORIZED.equals(statusCode) || HttpStatus.FORBIDDEN.equals(statusCode)) {
                        Long clientId = getClientId(request);

                        invalidateApiKey(clientId);

                        return Mono.error(new DocspaceApiKeyInvalidException(clientId));
                    }
                    return Mono.just(clientResponse);
                });
    }

    private Settings resolveSettings(final ClientRequest request) {
        Long clientId = getClientId(request);

        return settingsService.findByClientId(clientId);
    }

    private ClientRequest bearer(final ClientRequest request) {
        Settings settings = resolveSettings(request);
        String url = settings.getUrl();
        ApiKey apiKey = settings.getApiKey();

        if (Objects.isNull(url) || url.isEmpty()) {
            throw new DocspaceUrlNotFoundException(getClientId(request));
        }

        if (Objects.isNull(apiKey)) {
            throw new DocspaceApiKeyNotFoundException(getClientId(request));
        }

        if (!apiKey.isValid()) {
            throw new DocspaceApiKeyInvalidException(getClientId(request));
        }

        return ClientRequest.from(request)
                .url(getBaseUrl(settings, request))
                .headers(headers -> {
                    headers.setBearerAuth(apiKey.getValue());
                })
                .build();
    }

    private Mono<ClientRequest> mergeRequestAttributesIfNecessary(final ClientRequest request) {
        if (request.attribute(AUTHENTICATION_ATTR_NAME).isEmpty()) {
            return mergeRequestAttributesFromContext(request);
        }
        return Mono.just(request);
    }

    private Mono<ClientRequest> mergeRequestAttributesFromContext(final ClientRequest request) {
        ClientRequest.Builder builder = ClientRequest.from(request);
        return Mono.deferContextual(Mono::just)
                .cast(Context.class)
                .map(ctx -> builder.attributes(attrs -> populateRequestAttributes(attrs, ctx)))
                .map(ClientRequest.Builder::build);
    }

    private void populateRequestAttributes(final Map<String, Object> attrs, final Context ctx) {
        if (!ctx.hasKey(SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY)) {
            return;
        }

        Map<Object, Object> contextAttributes = ctx.get(SECURITY_REACTOR_CONTEXT_ATTRIBUTES_KEY);

        Authentication authentication = (Authentication) contextAttributes.get(Authentication.class);
        if (authentication != null) {
            attrs.putIfAbsent(AUTHENTICATION_ATTR_NAME, authentication);
        }
    }

    private Long getClientId(final ClientRequest request) {
        Map<String, Object> attrs = request.attributes();
        Authentication authentication = (Authentication) attrs.get(AUTHENTICATION_ATTR_NAME);

        User user = null;
        if (authentication != null) {
            if (authentication.getPrincipal() instanceof User) {
                user = (User) authentication.getPrincipal();
            }
            if (authentication.getPrincipal() instanceof OAuth2PipedriveUser) {
                return ((OAuth2PipedriveUser) authentication.getPrincipal()).getClientId();
            }
        }

        if (user != null) {
            return user.getClient().getId();
        }

        return null;
    }

    private URI getBaseUrl(final Settings settings, final ClientRequest request) {
        UriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(settings.getUrl());
        UriBuilder uriBuilder = uriBuilderFactory.uriString(request.url().toString());

        return uriBuilder.build();
    }

    private void invalidateApiKey(final Long clientId) {
        settingsService.setApiKeyValid(clientId, false);
    }
}
