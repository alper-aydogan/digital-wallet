package com.alper.digitalwallet.domain.repository;

import com.alper.digitalwallet.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAllByToWalletId(Long walletId);
}

