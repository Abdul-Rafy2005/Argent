package com.argent.common.exception;

public class ForbiddenException extends ArgentException {

    public ForbiddenException(String message) {
        super(message, "FORBIDDEN");
    }

    public ForbiddenException() {
        super("Forbidden", "FORBIDDEN");
    }
}
