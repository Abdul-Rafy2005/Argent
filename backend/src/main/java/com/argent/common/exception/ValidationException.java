package com.argent.common.exception;

public class ValidationException extends ArgentException {

    public ValidationException(String message) {
        super(message, "VALIDATION_ERROR");
    }

    public ValidationException(String message, String field) {
        super(message + " [" + field + "]", "VALIDATION_ERROR");
    }
}
