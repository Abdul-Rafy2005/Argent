package com.argent.common.exception;

public class NotFoundException extends ArgentException {

    public NotFoundException(String message) {
        super(message, "NOT_FOUND");
    }

    public NotFoundException(String entityType, String id) {
        super(entityType + " not found with id: " + id, "NOT_FOUND");
    }
}
