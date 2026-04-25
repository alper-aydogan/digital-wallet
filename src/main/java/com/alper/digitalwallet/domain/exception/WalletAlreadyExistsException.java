package com.alper.digitalwallet.domain.exception;

public class WalletAlreadyExistsException extends WalletException {
    public WalletAlreadyExistsException(String message) {
        super(message);
    }
}
