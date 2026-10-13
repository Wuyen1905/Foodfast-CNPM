package com.foodfast.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Handles all exceptions and returns structured error responses
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            WebRequest req
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null
                        ? error.getDefaultMessage()
                        : "Dữ liệu không hợp lệ")
                .orElse("Dữ liệu không hợp lệ");
        return errorBody(HttpStatus.BAD_REQUEST, new IllegalArgumentException(message), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex,
            WebRequest req
    ) {
        return errorBody(HttpStatus.BAD_REQUEST, ex, req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessConflict(
            IllegalStateException ex,
            WebRequest req
    ) {
        return errorBody(HttpStatus.CONFLICT, ex, req);
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(
            ForbiddenOperationException ex,
            WebRequest req
    ) {
        return errorBody(HttpStatus.FORBIDDEN, ex, req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            NoResourceFoundException ex,
            WebRequest req
    ) {
        return errorBody(HttpStatus.NOT_FOUND, ex, req);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception ex, WebRequest req) {
        LOGGER.error("Unhandled error while processing {}", req.getDescription(false), ex);
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, ex, req);
    }

    private ResponseEntity<Map<String, Object>> errorBody(
            HttpStatus status,
            Exception ex,
            WebRequest req
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", errorLabel(status));
        String message = status.is5xxServerError()
                ? "Đã xảy ra lỗi hệ thống. Vui lòng thử lại"
                : (ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi");
        body.put("message", message);
        body.put("path", req.getDescription(false).replace("uri=", ""));
        return ResponseEntity.status(status).body(body);
    }

    private String errorLabel(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Yêu cầu không hợp lệ";
            case CONFLICT -> "Xung đột nghiệp vụ";
            case FORBIDDEN -> "Không có quyền thực hiện";
            case NOT_FOUND -> "Không tìm thấy";
            default -> "Lỗi hệ thống";
        };
    }
}

