package com.vibe.jobs.admin.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

@ControllerAdvice(basePackages = "com.vibe.jobs.admin.web")
public class AdminExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AdminExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.error("HTTP message not readable", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "INVALID_REQUEST_BODY",
                "message", "请求体格式不正确: " + e.getMessage(),
                "details", e.getCause() != null ? e.getCause().getMessage() : "Unknown cause"
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation failed", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "VALIDATION_FAILED",
                "message", "请求参数验证失败",
                "details", e.getBindingResult().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("Argument type mismatch", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "TYPE_MISMATCH",
                "message", "参数类型不匹配: " + e.getMessage(),
                "parameter", e.getName(),
                "expectedType", e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "Unknown"
        ));
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleJsonProcessing(JsonProcessingException e) {
        log.error("JSON processing error", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "JSON_PROCESSING_ERROR",
                "message", "JSON 处理错误: " + e.getMessage(),
                "location", e.getLocation() != null ? e.getLocation().toString() : "Unknown"
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.error("Illegal argument", e);
        return ResponseEntity.badRequest().body(Map.of(
                "error", "ILLEGAL_ARGUMENT",
                "message", "参数错误: " + e.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unexpected error in admin controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "INTERNAL_ERROR", 
                "message", "服务器内部错误",
                "details", e.getMessage()
        ));
    }
}