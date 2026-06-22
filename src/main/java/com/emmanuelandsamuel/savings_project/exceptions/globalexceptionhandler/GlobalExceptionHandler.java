package com.emmanuelandsamuel.savings_project.exceptions.globalexceptionhandler;

import com.emmanuelandsamuel.savings_project.dtos.responses.ApiResponse;
import com.emmanuelandsamuel.savings_project.exceptions.ApplicationException;
import com.emmanuelandsamuel.savings_project.exceptions.WalletNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@SuppressWarnings("NullableProblems")
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // @Override
    // protected @Nullable ResponseEntity<Object>
    // handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders
    // headers, HttpStatusCode status, WebRequest request) {

    // List<String> validationErrorList = ex.getBindingResult()
    // .getAllErrors()
    // .stream()
    // .map(DefaultMessageSourceResolvable::getDefaultMessage)
    // .toList();

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiResponse<String>> handleApplicationException(final ApplicationException ex) {

        log.error("Application error: {}", ex.getMessage(), ex);
        ApiResponse<String> apiResponse = new ApiResponse<>(false, ex.getMessage(), ex.getMessage());

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleWalletNotFoundException(WalletNotFoundException ex) {

        log.error("Wallet not found: {}", ex.getMessage(), ex);

        ApiResponse<String> apiResponse = ApiResponse.error("Wallet not found.");

        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {

        log.error("Unexpected error", ex);

        ApiResponse<String> response = ApiResponse.error(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.error("Validation error: {}", errorMessage, ex);

        ApiResponse<String> apiResponse = ApiResponse.error(errorMessage);

        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

}
