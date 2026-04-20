package com.alper.digitalwallet.domain.repository;

import com.alper.digitalwallet.domain.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction transaction);

    List<Transaction> findAllByToWalletId(Long walletId);

    Page<Transaction> findAllByToWalletId(Long walletId, Pageable pageable);

    Page<Transaction> findAllByFromWalletIdOrToWalletId(Long walletId, Pageable pageable);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}

