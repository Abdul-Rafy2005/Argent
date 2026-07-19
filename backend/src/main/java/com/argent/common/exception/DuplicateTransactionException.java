package com.argent.common.exception;

public class DuplicateTransactionException extends ConflictException {

    public DuplicateTransactionException(String idempotencyKey) {
        super("Transaction", "idempotency key", idempotencyKey);
    }
}
