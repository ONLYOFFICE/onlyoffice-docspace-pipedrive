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

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DocspaceOAuth2StateRepository {
    private static final String KEY_PREFIX = "spring:docspace:oauth2:state:";
    private static final String VALUE_DELIMITER = "|";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveState(final String key, final String state, final String codeVerifier, final Duration ttl) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + key, state + VALUE_DELIMITER + codeVerifier, ttl);
    }

    public State getAndDeleteState(final String key) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(KEY_PREFIX + key);

        if (Objects.isNull(value)) {
            return null;
        }

        String[] parts = value.split("\\" + VALUE_DELIMITER, 2);

        return new State(parts[0], parts[1]);
    }

    public record State(String state, String codeVerifier) {
    }
}
