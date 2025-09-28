package com.nelsontr.paymentapp.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(ProblemType.RESOURCE_NOT_FOUND);
        pd.setTitle("Not Found");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request body");
        pd.setType(ProblemType.VALIDATION_ERROR);
        pd.setTitle("Bad Request");

        Map<String, List<String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.groupingBy(FieldError::getField,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
        if (!errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violations in request parameters");
        pd.setType(ProblemType.CONSTRAINT_VIOLATION);
        pd.setTitle("Bad Request");

        Map<String, List<String>> errors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "parameter";
            errors.computeIfAbsent(path, k -> new java.util.ArrayList<>()).add(v.getMessage());
        }
        if (!errors.isEmpty()) {
            pd.setProperty("errors", errors);
        }
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed JSON request");
        pd.setType(ProblemType.MALFORMED_JSON);
        pd.setTitle("Bad Request");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        pd.setType(ProblemType.INTERNAL_ERROR);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}
