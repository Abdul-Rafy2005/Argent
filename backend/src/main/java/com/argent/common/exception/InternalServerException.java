package com.argent.common.exception;

public class InternalServerException extends ArgentException {

    public InternalServerException(String message) {
        super(message, "INTERNAL_SERVER_ERROR");
    }

    public InternalServerException(String message, Throwable cause) {
        super(message, "INTERNAL_SERVER_ERROR", cause);
    }

    public InternalServerException() {
        super("An unexpected error occurred", "INTERNAL_SERVER_ERROR");
    }
}
