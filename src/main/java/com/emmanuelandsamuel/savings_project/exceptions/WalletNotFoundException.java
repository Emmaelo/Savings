package com.emmanuelandsamuel.savings_project.exceptions;


public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException(String message) {
        super(message);
    }

}
