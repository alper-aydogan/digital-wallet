package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTransactionsUseCase {

    private final TransactionRepository transactionRepository;

    public List<Transaction> execute(Long walletId) {
        return transactionRepository.findAllByToWalletId(walletId);
    }

    public Page<Transaction> execute(Long walletId, Pageable pageable) {
        return transactionRepository.findAllByToWalletId(walletId, pageable);
    }
}

