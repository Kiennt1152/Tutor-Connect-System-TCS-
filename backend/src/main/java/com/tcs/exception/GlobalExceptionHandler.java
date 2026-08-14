package com.tcs.exception;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Giá trị không hợp lệ",
                        (a, b) -> a,
                        LinkedHashMap::new));

        String firstMessage = fieldErrors.values().stream().findFirst().orElse("Dữ liệu không hợp lệ");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", firstMessage);
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", "Kích thước tệp vượt quá giới hạn cho phép");
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, String>> handleBusiness(BusinessException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(VerificationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleVerificationRequired(
            VerificationRequiredException exception) {
        Map<String, String> body = new HashMap<>();
        body.put("message", exception.getMessage());
        body.put("code", VerificationRequiredException.CODE); // frontend dựa vào code để điều hướng
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // Log nguyên nhân gốc (tên cột/constraint) để dễ chẩn đoán.
        log.error("DataIntegrityViolation:", exception);
        String root = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage() : exception.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("message", "Dữ liệu không hợp lệ hoặc bị trùng: "
                + (root != null ? root : "Vui lòng kiểm tra lại."));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /** Bắt mọi lỗi ngoài dự kiến (500) -> trả message rõ ràng + ghi log để dễ chẩn đoán. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        log.error("Loi khong xac dinh:", exception);
        String msg = exception.getMessage();
        Map<String, String> body = new HashMap<>();
        body.put("message", "Có lỗi hệ thống: "
                + (msg != null && !msg.isBlank() ? msg : exception.getClass().getSimpleName()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
