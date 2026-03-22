package com.example.shortlink.handler;

import com.example.shortlink.model.Result;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = DuplicateKeyException.class)
    public Result handleDuplicateKeyException(Exception e) {
        return Result.failure("Duplicate key error: " + e.getMessage());
    }

    @ExceptionHandler(value = Exception.class)
    public Result handleException(Exception e) {
        return Result.failure("Internal server error: " + e.getMessage());
    }
}
