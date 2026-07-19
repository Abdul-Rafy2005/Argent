package com.argent.common.exception;

public class ConflictException extends ArgentException {

    public ConflictException(String message) {
        super(message, "CONFLICT");
    }

    public ConflictException(String entityType, String field, String value) {
        super(entityType + " already exists with " + field + ": " + value, "CONFLICT");
    }
}
