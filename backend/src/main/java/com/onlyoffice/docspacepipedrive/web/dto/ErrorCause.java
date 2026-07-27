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

package com.onlyoffice.docspacepipedrive.web.dto;

/**
 * Stable, compiler-checked identifiers for the {@code cause} field of {@link ErrorResponse}.
 * These values are part of the HTTP API contract consumed by the frontend, so they must not be
 * derived from Java class names (e.g. via reflection) - renaming an internal exception class must
 * never change the wire format.
 */
public enum ErrorCause {
    PIPEDRIVE_AUTHENTICATION_REQUIRED("PipedriveAuthenticationRequired"),
    PIPEDRIVE_OAUTH2_AUTHORIZATION_EXCEPTION("PipedriveOAuth2AuthorizationException"),
    PIPEDRIVE_WEB_CLIENT_RESPONSE_EXCEPTION("PipedriveWebClientResponseException"),
    DOCSPACE_OAUTH2_AUTHORIZATION_EXCEPTION("DocspaceOAuth2AuthorizationException"),
    DOCSPACE_WEB_CLIENT_RESPONSE_EXCEPTION("DocspaceWebClientResponseException"),
    DOCSPACE_URL_NOT_FOUND_EXCEPTION("DocspaceUrlNotFoundException"),
    DOCSPACE_ACCOUNT_ALREADY_EXISTS_EXCEPTION("DocspaceAccountAlreadyExistsException"),
    DOCSPACE_ACCOUNT_NOT_FOUND_EXCEPTION("DocspaceAccountNotFoundException"),
    SETTINGS_VALIDATION_EXCEPTION("SettingsValidationException"),
    DOCSPACE_API_KEY_NOT_FOUND_EXCEPTION("DocspaceApiKeyNotFoundException"),
    DOCSPACE_API_KEY_INVALID_EXCEPTION("DocspaceApiKeyInvalidException"),
    DOCSPACE_OAUTH2_STATE_EXCEPTION("DocspaceOAuth2StateException"),
    REQUEST_ACCESS_TO_ROOM_EXCEPTION("RequestAccessToRoomException");

    private final String wireValue;

    ErrorCause(final String wireValue) {
        this.wireValue = wireValue;
    }

    public String getWireValue() {
        return wireValue;
    }
}
