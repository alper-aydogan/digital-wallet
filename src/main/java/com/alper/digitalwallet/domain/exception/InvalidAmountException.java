package com.alper.digitalwallet.domain.exception;

public class InvalidAmountException extends WalletException {
    public InvalidAmountException(String message) {
        super(message);
    }
}
