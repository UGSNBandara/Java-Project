package com.codejam.codex.authzen.exceptions;

import com.codejam.codex.authzen.responses.AuthzenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AuthzenExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        return buildResponse("Unauthorized: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        return buildResponse("Forbidden: " + ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleCredentialsNotFound(AuthenticationCredentialsNotFoundException ex) {
        return buildResponse("Credentials missing: " + ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildResponse("Bad request: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleIllegalState(IllegalStateException ex) {
        return buildResponse("Conflict: " + ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AuthzenResponse<Object>> handleRuntime(RuntimeException ex) {
        return buildResponse("Unexpected error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthzenResponse<Object>> handleGeneric(Exception ex) {
        return buildResponse("Internal server error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<AuthzenResponse<Object>> buildResponse(String message, HttpStatus status) {
        AuthzenResponse<Object> response = new AuthzenResponse<>(null, false, message);
        return ResponseEntity.status(status).body(response);
    }

}
