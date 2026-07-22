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

package com.onlyoffice.docspacepipedrive.web.controller;

import com.onlyoffice.docspacepipedrive.exceptions.DocspaceAccountAlreadyExistsException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceAccountNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyInvalidException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceApiKeyNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceOAuth2AuthorizationException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceOAuth2StateException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceUrlNotFoundException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceWebClientResponseException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveOAuth2AuthorizationException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveWebClientResponseException;
import com.onlyoffice.docspacepipedrive.exceptions.RequestAccessToRoomException;
import com.onlyoffice.docspacepipedrive.exceptions.SettingsValidationException;
import com.onlyoffice.docspacepipedrive.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(PipedriveWebClientResponseException.class)
    public ResponseEntity<ErrorResponse> pipedriveWebClientResponseException(PipedriveWebClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        new ErrorResponse(
                                PipedriveWebClientResponseException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(PipedriveOAuth2AuthorizationException.class)
    public ResponseEntity<ErrorResponse> pipedriveOAuth2AuthorizationException(
            PipedriveOAuth2AuthorizationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ErrorResponse(
                                PipedriveOAuth2AuthorizationException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceWebClientResponseException.class)
    public ResponseEntity<ErrorResponse> docspaceWebClientResponseException(DocspaceWebClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        new ErrorResponse(
                                DocspaceWebClientResponseException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceUrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> docspaceUrlNotFoundException(DocspaceUrlNotFoundException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        new ErrorResponse(
                                DocspaceUrlNotFoundException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceAccountAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> docspaceAccountAlreadyExists(DocspaceAccountAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                DocspaceAccountAlreadyExistsException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceAccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> docspaceAccountNotFoundException(DocspaceAccountNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponse(
                                DocspaceAccountNotFoundException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(SettingsValidationException.class)
    public ResponseEntity<ErrorResponse> settingsValidationException(
            SettingsValidationException e) {
        Map<String, Object> params = new HashMap<>();
        params.put("validationError", e.getErrorCode().toString());
        params.put("validationMessage", e.getErrorCode().getMessage());
        params.putAll(e.getParams());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                SettingsValidationException.class.getSimpleName(),
                                e.getMessage(),
                                params
                        )
                );
    }

    @ExceptionHandler(DocspaceApiKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> docspaceApiKeyNotFoundException(DocspaceApiKeyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        new ErrorResponse(
                                DocspaceApiKeyNotFoundException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceApiKeyInvalidException.class)
    public ResponseEntity<ErrorResponse> docspaceApiKeyInvalidException(DocspaceApiKeyInvalidException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(
                        new ErrorResponse(
                                DocspaceApiKeyInvalidException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceOAuth2StateException.class)
    public ResponseEntity<ErrorResponse> docspaceOAuth2StateException(DocspaceOAuth2StateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        new ErrorResponse(
                                DocspaceOAuth2StateException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(DocspaceOAuth2AuthorizationException.class)
    public ResponseEntity<ErrorResponse> docspaceOAuth2AuthorizationException(
            DocspaceOAuth2AuthorizationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ErrorResponse(
                                DocspaceOAuth2AuthorizationException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }

    @ExceptionHandler(RequestAccessToRoomException.class)
    public ResponseEntity<ErrorResponse> requestAccessToRoomException(RequestAccessToRoomException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                RequestAccessToRoomException.class.getSimpleName(),
                                e.getLocalizedMessage(),
                                null
                        )
                );
    }
}
