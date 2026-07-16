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

import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;


public class PipedriveWebClientResponseException extends WebClientResponseException {
    public PipedriveWebClientResponseException(final WebClientResponseException e) {
        super(
                e.getMessage(),
                e.getStatusCode(),
                e.getStatusText(),
                e.getHeaders(),
                e.getResponseBodyAsByteArray(),
                StandardCharsets.UTF_8,
                e.getRequest()
        );
    }
}
