package fu.edu.mss301.digilib.fine.api.controller;

import fu.edu.mss301.digilib.fine.api.dto.ApiErrorResponse;
import fu.edu.mss301.digilib.fine.application.exception.BusinessConflictException;
import fu.edu.mss301.digilib.fine.application.exception.ForbiddenException;
import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookException;
import fu.edu.mss301.digilib.fine.application.exception.InvalidWebhookSignatureException;
import fu.edu.mss301.digilib.fine.application.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BusinessConflictException.class)
    ResponseEntity<ApiErrorResponse> handleConflict(BusinessConflictException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidWebhookException.class)
    ResponseEntity<ApiErrorResponse> handleBadWebhook(InvalidWebhookException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    ResponseEntity<ApiErrorResponse> handleUnauthorizedWebhook(
            InvalidWebhookSignatureException exception
    ) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiErrorResponse> handleConfiguration(IllegalStateException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                LocalDateTime.now()
        ));
    }
}
