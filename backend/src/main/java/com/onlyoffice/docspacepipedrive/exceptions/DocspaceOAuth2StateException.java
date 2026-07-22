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

public class DocspaceOAuth2StateException extends RuntimeException {
    public DocspaceOAuth2StateException(final Reason reason) {
        super(reason.getMessage());
    }

    public enum Reason {
        EXPIRED("DocSpace OAuth2 state has expired or was already used"),
        INVALID("DocSpace OAuth2 state does not match");

        private final String message;

        Reason(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
