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

import com.onlyoffice.docspacepipedrive.client.docspace.DocspaceClient;
import com.onlyoffice.docspacepipedrive.client.docspace.dto.DocspaceUser;
import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.Settings;
import com.onlyoffice.docspacepipedrive.entity.user.AccessToken;
import com.onlyoffice.docspacepipedrive.entity.user.RefreshToken;
import com.onlyoffice.docspacepipedrive.events.user.DocspaceLoginUserEvent;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceAccountNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceOAuth2AuthorizationException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceOAuth2StateException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceUrlNotFoundException;
import com.onlyoffice.docspacepipedrive.security.oauth.DocspaceOAuth2StateRepository;
import com.onlyoffice.docspacepipedrive.service.DocspaceAccountService;
import com.onlyoffice.docspacepipedrive.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class DocspaceOAuth2Manager {
    private static final String AUTHORIZATION_URI_PATH = "/oauth2/authorize";
    private static final String TOKEN_URI_PATH = "/oauth2/token";
    private static final int CODE_VERIFIER_BYTE_LENGTH = 64;
    private static final Duration ACCESS_TOKEN_EXPIRY_BUFFER = Duration.ofSeconds(60);

    private final SettingsService settingsService;
    private final DocspaceAccountService docspaceAccountService;
    private final DocspaceClient docspaceClient;
    private final DocspaceOAuth2StateRepository docspaceOAuth2StateRepository;
    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
            docspaceAccessTokenResponseClient;
    private final OAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest>
            docspaceRefreshTokenResponseClient;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.docspace-client-id}")
    private String docspaceClientId;
    @Value("${app.docspace-client-secret}")
    private String docspaceClientSecret;
    @Value("${app.docspace-oauth-redirect-uri}")
    private String docspaceOAuthRedirectUri;
    @Value("${app.docspace-oauth-state-ttl}")
    private String docspaceOAuthStateTtl;

    public String buildAuthorizeUrl(final Long clientId, final Long userId) {
        String docspaceUrl = getDocspaceUrl(clientId);

        String state = UUID.randomUUID().toString();
        String codeVerifier = generateCodeVerifier();

        docspaceOAuth2StateRepository.saveState(
                stateKey(clientId, userId),
                state,
                codeVerifier,
                Duration.parse(docspaceOAuthStateTtl)
        );

        return UriComponentsBuilder.fromUriString(docspaceUrl)
                .path(AUTHORIZATION_URI_PATH)
                .queryParam("client_id", docspaceClientId)
                .queryParam("redirect_uri", docspaceOAuthRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .queryParam(PkceParameterNames.CODE_CHALLENGE, generateCodeChallenge(codeVerifier))
                .queryParam(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256")
                .build()
                .toUriString();
    }

    public void handleCallback(final Long clientId, final Long userId, final String code, final String state) {
        DocspaceOAuth2StateRepository.State savedState =
                docspaceOAuth2StateRepository.getAndDeleteState(stateKey(clientId, userId));

        if (Objects.isNull(savedState)) {
            throw new DocspaceOAuth2StateException(DocspaceOAuth2StateException.Reason.EXPIRED);
        }

        if (!savedState.state().equals(state)) {
            throw new DocspaceOAuth2StateException(DocspaceOAuth2StateException.Reason.INVALID);
        }

        String docspaceUrl = getDocspaceUrl(clientId);
        ClientRegistration clientRegistration = buildClientRegistration(docspaceUrl);

        OAuth2AccessTokenResponse tokenResponse = exchangeCodeForToken(
                clientRegistration, code, state, savedState.codeVerifier()
        );

        AccessToken accessToken = AccessToken.builder()
                .value(tokenResponse.getAccessToken().getTokenValue())
                .issuedAt(tokenResponse.getAccessToken().getIssuedAt())
                .expiresAt(tokenResponse.getAccessToken().getExpiresAt())
                .build();

        RefreshToken refreshToken = null;
        if (Objects.nonNull(tokenResponse.getRefreshToken())) {
            refreshToken = RefreshToken.builder()
                    .value(tokenResponse.getRefreshToken().getTokenValue())
                    .issuedAt(tokenResponse.getRefreshToken().getIssuedAt())
                    .build();
        }

        DocspaceUser docspaceUser = docspaceClient.getUserByAccessToken(docspaceUrl, accessToken.getValue());

        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(clientId, userId);
        if (Objects.isNull(docspaceAccount)) {
            docspaceAccount = new DocspaceAccount();
        }

        docspaceAccount.setUuid(docspaceUser.getId());
        docspaceAccount.setEmail(docspaceUser.getEmail());
        docspaceAccount.setAccessToken(accessToken);
        docspaceAccount.setRefreshToken(refreshToken);

        DocspaceAccount savedDocspaceAccount = docspaceAccountService.save(clientId, userId, docspaceAccount);

        eventPublisher.publishEvent(new DocspaceLoginUserEvent(this, savedDocspaceAccount));
    }

    public String getValidAccessToken(final Long clientId, final Long userId) {
        DocspaceAccount docspaceAccount = docspaceAccountService.findByClientIdAndUserId(clientId, userId);

        if (Objects.isNull(docspaceAccount) || Objects.isNull(docspaceAccount.getAccessToken())) {
            throw new DocspaceAccountNotFoundException(clientId, userId);
        }

        AccessToken accessToken = docspaceAccount.getAccessToken();
        RefreshToken refreshToken = docspaceAccount.getRefreshToken();

        if (!isExpiringSoon(accessToken) || Objects.isNull(refreshToken)) {
            return accessToken.getValue();
        }

        String docspaceUrl = getDocspaceUrl(clientId);
        ClientRegistration clientRegistration = buildClientRegistration(docspaceUrl);

        OAuth2AccessTokenResponse tokenResponse =
                refreshAccessToken(clientRegistration, accessToken, refreshToken);

        AccessToken refreshedAccessToken = AccessToken.builder()
                .value(tokenResponse.getAccessToken().getTokenValue())
                .issuedAt(tokenResponse.getAccessToken().getIssuedAt())
                .expiresAt(tokenResponse.getAccessToken().getExpiresAt())
                .build();

        RefreshToken refreshedRefreshToken = refreshToken;
        if (Objects.nonNull(tokenResponse.getRefreshToken())) {
            refreshedRefreshToken = RefreshToken.builder()
                    .value(tokenResponse.getRefreshToken().getTokenValue())
                    .issuedAt(tokenResponse.getRefreshToken().getIssuedAt())
                    .build();
        }

        docspaceAccount.setAccessToken(refreshedAccessToken);
        docspaceAccount.setRefreshToken(refreshedRefreshToken);
        docspaceAccountService.save(clientId, userId, docspaceAccount);

        return refreshedAccessToken.getValue();
    }

    private boolean isExpiringSoon(final AccessToken accessToken) {
        if (Objects.isNull(accessToken.getExpiresAt())) {
            return false;
        }

        return Instant.now().plus(ACCESS_TOKEN_EXPIRY_BUFFER).isAfter(accessToken.getExpiresAt());
    }

    private OAuth2AccessTokenResponse refreshAccessToken(final ClientRegistration clientRegistration,
                                                          final AccessToken accessToken,
                                                          final RefreshToken refreshToken) {
        OAuth2AccessToken currentAccessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessToken.getValue(),
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt()
        );

        OAuth2RefreshToken currentRefreshToken = new OAuth2RefreshToken(
                refreshToken.getValue(),
                refreshToken.getIssuedAt()
        );

        OAuth2RefreshTokenGrantRequest grantRequest = new OAuth2RefreshTokenGrantRequest(
                clientRegistration, currentAccessToken, currentRefreshToken
        );

        try {
            return docspaceRefreshTokenResponseClient.getTokenResponse(grantRequest);
        } catch (OAuth2AuthorizationException e) {
            throw new DocspaceOAuth2AuthorizationException(e);
        }
    }

    private OAuth2AccessTokenResponse exchangeCodeForToken(final ClientRegistration clientRegistration,
                                                            final String code, final String state,
                                                            final String codeVerifier) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri())
                .clientId(clientRegistration.getClientId())
                .redirectUri(clientRegistration.getRedirectUri())
                .state(state)
                .attributes(Map.of(PkceParameterNames.CODE_VERIFIER, codeVerifier))
                .build();

        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success(code)
                .redirectUri(clientRegistration.getRedirectUri())
                .state(state)
                .build();

        OAuth2AuthorizationCodeGrantRequest grantRequest = new OAuth2AuthorizationCodeGrantRequest(
                clientRegistration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );

        try {
            return docspaceAccessTokenResponseClient.getTokenResponse(grantRequest);
        } catch (OAuth2AuthorizationException e) {
            throw new DocspaceOAuth2AuthorizationException(e);
        }
    }

    private ClientRegistration buildClientRegistration(final String docspaceUrl) {
        return ClientRegistration.withRegistrationId("docspace")
                .clientId(docspaceClientId)
                .clientSecret(docspaceClientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(docspaceOAuthRedirectUri)
                .authorizationUri(docspaceUrl + AUTHORIZATION_URI_PATH)
                .tokenUri(docspaceUrl + TOKEN_URI_PATH)
                .build();
    }

    private String getDocspaceUrl(final Long clientId) {
        Settings settings = settingsService.findByClientId(clientId);
        String docspaceUrl = settings.getUrl();

        if (Objects.isNull(docspaceUrl) || docspaceUrl.isEmpty()) {
            throw new DocspaceUrlNotFoundException(clientId);
        }

        return docspaceUrl;
    }

    private String stateKey(final Long clientId, final Long userId) {
        return clientId + ":" + userId;
    }

    private String generateCodeVerifier() {
        byte[] codeVerifierBytes = new byte[CODE_VERIFIER_BYTE_LENGTH];
        new SecureRandom().nextBytes(codeVerifierBytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes);
    }

    private String generateCodeChallenge(final String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));

            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
