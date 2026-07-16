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

package com.onlyoffice.docspacepipedrive.security.util;

import org.springframework.stereotype.Component;

import java.util.Random;


@Component
public final class RandomPasswordGenerator {
    public static final String UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    public static final String NUMBERS = "1234567890";
    public static final String SPECIAL_CHARS = "!@#$%^&*()_+{}";

    private RandomPasswordGenerator() {
    }

    public static String generatePassword(final int length) {
        char[] password = new char[length];
        String charSet = "";
        Random random = new Random();

        charSet = charSet.concat(UPPER_CHARS)
                .concat(LOWER_CHARS)
                .concat(NUMBERS)
                .concat(SPECIAL_CHARS);

        for (int i = 0; i < length; i++) {
            password[i] = charSet.toCharArray()[random.nextInt(charSet.length() - 1)];
        }

        return String.valueOf(password);
    }
}
