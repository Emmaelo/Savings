package com.emmanuelandsamuel.savings_project.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApplicationException extends RuntimeException {

    private final String errorMessage;
}
