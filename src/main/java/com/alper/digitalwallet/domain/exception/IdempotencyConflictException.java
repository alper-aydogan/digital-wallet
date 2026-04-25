package com.alper.digitalwallet.domain.exception;

public class IdempotencyConflictException extends WalletException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
