package com.argent.common.exception;

public class UnauthorizedException extends ArgentException {

    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED");
    }

    public UnauthorizedException() {
        super("Unauthorized", "UNAUTHORIZED");
    }
}
