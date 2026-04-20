package com.alper.digitalwallet.domain.exception;

public class InsufficientBalanceException extends WalletException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

