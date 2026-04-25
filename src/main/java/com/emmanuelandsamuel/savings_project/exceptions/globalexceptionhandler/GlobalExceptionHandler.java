package com.emmanuelandsamuel.savings_project.exceptions.globalexceptionhandler;

import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@SuppressWarnings("NullableProblems")
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<String> validationErrorList = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        ApiResponse<List<String>> validationErrors = ApiResponse.validationError("Validation failed", validationErrorList);

        return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(Exception ex) {

        log.error("An unexpected error occurred: {}", ex.getMessage(), ex);

        ApiResponse<String> apiResponse = ApiResponse.error("An unexpected error occurred on the server, please try again later.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<String>> handleApplicationException(ApplicationException ex) {

        log.error("Application error: {}", ex.getMessage(), ex);

        ApiResponse<String> apiResponse = ApiResponse.error("An error occurred while processing your request.");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
}
