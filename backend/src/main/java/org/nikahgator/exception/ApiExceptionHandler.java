package org.nikahgator.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> handle(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException e) {
        String fields = e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).distinct()
                .reduce((a, b) -> a + ", " + b).orElse("input");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Please check: " + fields));
    }
}
