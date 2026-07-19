package com.argent.common.exception;

public class OptimisticLockingException extends ArgentException {

    public OptimisticLockingException(String message) {
        super(message, "OPTIMISTIC_LOCKING_FAILURE");
    }

    public OptimisticLockingException() {
        super("Concurrent modification detected. Please retry the operation.", "OPTIMISTIC_LOCKING_FAILURE");
    }
}
