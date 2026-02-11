package com.apexvision.optimizer.advice;

import com.apexvision.optimizer.exception.BusinessRuleException;
import com.apexvision.optimizer.exception.OptimizationCalculationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        log.warn("Business Rule Violation: {}", ex.getMessage());
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Business Rule Violation",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(OptimizationCalculationException.class)
    public ProblemDetail handleOptimizationException(OptimizationCalculationException ex, WebRequest request) {
        log.error("Critical Optimization Failure: ", ex);
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Optimization Calculation Error",
                "A critical error occurred during route calculation. Please retry or contact support.",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal Argument: {}", ex.getMessage());
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation Error: {}", detail);
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                detail,
                request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON Request: {}", ex.getMessage());
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON / Type Mismatch",
                "Invalid data format in request body. Ensure numeric fields (like ID) are not strings.",
                request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected Error: ", ex);
        return ErrorResponseFactory.createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Trace ID logged.",
                request);
    }
}
