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

package com.onlyoffice.docspacepipedrive.exceptions;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class SettingsValidationException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> params;

    public SettingsValidationException(final ErrorCode errorCode) {
        this(errorCode, new HashMap<>());
    }

    public SettingsValidationException(final ErrorCode errorCode, final Map<String, Object> params) {
        super(errorCode.getMessage());

        this.errorCode = errorCode;
        this.params = params;
    }

    public enum ErrorCode {
        DOCSPACE_CAN_NOT_BE_REACHED("DocSpace can not be reached"),
        DOCSPACE_API_KEY_IS_INVALID("DocSpace API Key is invalid"),
        DOCSPACE_API_KEY_IS_NOT_CONTAINS_REQUIRED_SCOPES("DocSpace API Key is not contains required scopes"),
        DOCSPACE_API_KEY_OWNER_IS_NOT_ADMIN("DocSpace API Key owner is not DocSpace Admin");

        private final String message;

        ErrorCode(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
