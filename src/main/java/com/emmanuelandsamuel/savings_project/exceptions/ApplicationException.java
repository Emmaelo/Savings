package com.emmanuelandsamuel.savings_project.exceptions;

public class ApplicationException extends RuntimeException {
    public ApplicationException(String errorMessage) {
        super(errorMessage);
       
    }
}
