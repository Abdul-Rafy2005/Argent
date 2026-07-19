package com.argent.common.exception;

import lombok.Getter;

@Getter
public abstract class ArgentException extends RuntimeException {

    private final String code;

    protected ArgentException(String message, String code) {
        super(message);
        this.code = code;
    }

    protected ArgentException(String message, String code, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
