package com.maka.support;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> permission(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of("success",false,"code",error.getStatus().value(),
            "message",error.getReason() == null ? "请求未完成" : error.getReason()));
    }
}
