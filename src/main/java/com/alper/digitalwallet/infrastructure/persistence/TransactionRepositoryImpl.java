package com.alper.digitalwallet.infrastructure.persistence;

import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}

