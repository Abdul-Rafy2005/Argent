package com.argent.common.exception;

public class WalletClosedException extends ArgentException {
    public WalletClosedException(String walletId) {
        super("Wallet " + walletId + " is closed and cannot accept transactions", "WALLET_CLOSED");
    }
}
