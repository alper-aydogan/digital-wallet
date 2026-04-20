package com.alper.digitalwallet.infrastructure.persistence;

import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final JpaTransactionRepository jpaTransactionRepository;

    @Override
    public Transaction save(Transaction transaction) {
        return jpaTransactionRepository.save(transaction);
    }

    @Override
    public List<Transaction> findAllByToWalletId(Long walletId) {
        return jpaTransactionRepository.findAllByToWalletId(walletId);
    }

    @Override
    public Page<Transaction> findAllByToWalletId(Long walletId, Pageable pageable) {
        return jpaTransactionRepository.findAllByToWalletId(walletId, pageable);
    }

    @Override
    public Page<Transaction> findAllByFromWalletIdOrToWalletId(Long walletId, Pageable pageable) {
        return jpaTransactionRepository.findAllByFromWalletIdOrToWalletId(walletId, pageable);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return jpaTransactionRepository.findByIdempotencyKey(idempotencyKey);
    }
}

