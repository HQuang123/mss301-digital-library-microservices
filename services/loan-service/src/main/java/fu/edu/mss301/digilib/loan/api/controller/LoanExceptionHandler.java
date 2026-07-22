package fu.edu.mss301.digilib.loan.api.controller;

import fu.edu.mss301.digilib.loan.application.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class LoanExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> notFoundOrInvalid(IllegalArgumentException exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> conflict(IllegalStateException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Yêu cầu không hợp lệ");
        return response(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(DownstreamServiceException.class)
    ResponseEntity<Map<String, Object>> downstreamService(DownstreamServiceException exception) {
        return response(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", vietnameseError(status),
                "message", message == null ? vietnameseError(status) : message));
    }

    private String vietnameseError(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case CONFLICT -> "Xung đột dữ liệu";
            case BAD_GATEWAY -> "Không thể kết nối dịch vụ phụ thuộc";
            default -> "Đã xảy ra lỗi";
        };
    }
}
