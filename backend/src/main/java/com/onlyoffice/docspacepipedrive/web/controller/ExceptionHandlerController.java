package com.onlyoffice.docspacepipedrive.web.controller;

import com.onlyoffice.docspacepipedrive.exceptions.DocspaceAccessDeniedException;
import com.onlyoffice.docspacepipedrive.exceptions.DocspaceWebClientResponseException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveAccessDeniedException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveOAuth2AuthorizationException;
import com.onlyoffice.docspacepipedrive.exceptions.PipedriveWebClientResponseException;
import com.onlyoffice.docspacepipedrive.exceptions.RoomNotFoundException;
import com.onlyoffice.docspacepipedrive.web.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class ExceptionHandlerController {
    @ExceptionHandler(RoomNotFoundException.class)
    public ResponseEntity<ErrorResponse> entityNotFound(RoomNotFoundException e) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        new ErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.INTEGRATION_APP
                        )
                );
    }

    @ExceptionHandler(PipedriveWebClientResponseException.class)
    public ResponseEntity<ErrorResponse> pipedriveWebClientResponseException(PipedriveWebClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        new ErrorResponse(
                                e.getStatusCode().value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.PIPEDRIVE
                        )
                );
    }

    @ExceptionHandler(PipedriveAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> pipedriveAccessDeniedException(PipedriveAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.PIPEDRIVE
                        )
                );
    }

    @ExceptionHandler(PipedriveOAuth2AuthorizationException.class)
    public ResponseEntity<ErrorResponse> pipedriveOAuth2AuthorizationException(PipedriveOAuth2AuthorizationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ErrorResponse(
                                HttpStatus.UNAUTHORIZED.value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.PIPEDRIVE
                        )
                );
    }

    @ExceptionHandler(DocspaceWebClientResponseException.class)
    public ResponseEntity<ErrorResponse> docspaceWebClientResponseException(DocspaceWebClientResponseException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        new ErrorResponse(
                                e.getStatusCode().value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.DOCSPACE
                        )
                );
    }

    @ExceptionHandler(DocspaceAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> docspaceAccessDeniedException(DocspaceAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        new ErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                e.getLocalizedMessage(),
                                ErrorResponse.Provider.DOCSPACE
                        )
                );
    }
}
