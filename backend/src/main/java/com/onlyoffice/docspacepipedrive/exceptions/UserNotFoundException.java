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

import java.text.MessageFormat;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(final Long id) {
        super(MessageFormat.format(
                "User with ID ({0}) not found.",
                id.toString()
        ));
    }

    public UserNotFoundException(final Long clientId, final Long userId) {
        super(MessageFormat.format(
                "User with CLIENT_ID ({0}) and USER_ID ({1}) not found.",
                clientId.toString(),
                userId.toString()
        ));
    }
}
