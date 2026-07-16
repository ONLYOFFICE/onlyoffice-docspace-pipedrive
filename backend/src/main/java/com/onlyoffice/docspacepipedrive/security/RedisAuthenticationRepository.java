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

package com.onlyoffice.docspacepipedrive.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class RedisAuthenticationRepository {
    private static final String KEY_PREFIX = "spring:authentication:";

    private final RedisTemplate<String, Authentication> authenticationRedisTemplate;

    public Authentication getAuthentication(final String username, final String credentials) {
        Authentication authentication = authenticationRedisTemplate.opsForValue().get(KEY_PREFIX + username);

        if (Objects.isNull(authentication)) {
            return null;
        }

        if (!credentials.equals(authentication.getCredentials())) {
            return null;
        }

        return authentication;
    }

    public void saveAuthentication(final String username, final Authentication value, final Instant expiresAt) {
        authenticationRedisTemplate.opsForValue().set(
                KEY_PREFIX + username,
                value,
                Duration.between(Instant.now(), expiresAt)
        );
    }

    public void deleteAuthentication(final String username) {
        authenticationRedisTemplate.delete(KEY_PREFIX + username);
    }
}
