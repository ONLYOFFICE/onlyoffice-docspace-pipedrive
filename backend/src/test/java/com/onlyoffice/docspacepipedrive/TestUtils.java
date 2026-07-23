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

package com.onlyoffice.docspacepipedrive;

import com.onlyoffice.docspacepipedrive.entity.DocspaceAccount;
import com.onlyoffice.docspacepipedrive.entity.User;
import com.onlyoffice.docspacepipedrive.entity.user.AccessToken;
import com.onlyoffice.docspacepipedrive.entity.user.RefreshToken;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.UUID;


public final class TestUtils {
    private TestUtils() {
    }
    public static User createUser(final Long userId, final Long clientId) {
        return User.builder()
                .userId(userId)
                .accessToken(createAccessToken(userId, clientId))
                .refreshToken(createRefreshToken(userId, clientId))
                .build();
    }

    public static AccessToken createAccessToken(final Long userId, final Long clientId) {
        Instant now = Instant.now();

        return AccessToken.builder()
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .value(MessageFormat.format("access_token_{0}_{1}", clientId.toString(), userId.toString()))
                .build();
    }

    public static RefreshToken createRefreshToken(final Long userId, final Long clientId) {
        Instant now = Instant.now();

        return RefreshToken.builder()
                .issuedAt(now)
                .value(MessageFormat.format("refresh_token_{0}_{1}", clientId.toString(), userId.toString()))
                .build();
    }

    public static DocspaceAccount createDocspaceAccount(final Long userId) {
        UUID uuid = UUID.fromString(MessageFormat.format(
                "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa{0}",
                userId.toString()
        ));

        return DocspaceAccount.builder()
                .uuid(uuid)
                .build();
    }
}
