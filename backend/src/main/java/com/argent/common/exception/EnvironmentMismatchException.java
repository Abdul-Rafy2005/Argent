package com.argent.common.exception;

public class EnvironmentMismatchException extends ArgentException {
    public EnvironmentMismatchException(String message) {
        super(message, "ENVIRONMENT_MISMATCH");
    }

    public EnvironmentMismatchException(String resourceEnvironment, String keyEnvironment) {
        super("Cannot access " + resourceEnvironment + " resource with " + keyEnvironment + " API key",
                "ENVIRONMENT_MISMATCH");
    }
}
