package com.argent.common.exception;

public class WalletFrozenException extends ArgentException {
    public WalletFrozenException(String walletId) {
        super("Wallet " + walletId + " is frozen and cannot accept transactions", "WALLET_FROZEN");
    }
}
