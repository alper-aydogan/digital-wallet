package com.alper.digitalwallet.domain.model;

public enum TransactionType {
    DEPOSIT("Para Yatırma"),
    WITHDRAWAL("Para Çekme"),
    TRANSFER("Para Transferi");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
