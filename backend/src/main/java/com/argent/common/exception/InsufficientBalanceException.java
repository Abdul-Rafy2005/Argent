package com.argent.common.exception;

public class InsufficientBalanceException extends ArgentException {

    public InsufficientBalanceException(String message) {
        super(message, "INSUFFICIENT_BALANCE");
    }

    public InsufficientBalanceException(String walletId, java.math.BigDecimal available, java.math.BigDecimal requested) {
        super("Wallet " + walletId + " has insufficient balance. Available: " + available + ", Requested: " + requested, "INSUFFICIENT_BALANCE");
    }
}
