package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetTransactionsUseCase {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public List<Transaction> execute(Long walletId) {
        return transactionRepository.findAllByToWalletId(walletId);
    }

    public Page<Transaction> execute(Long walletId, Pageable pageable) {
        // Cüzdan var mı kontrol et
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Cuzdan bulunamadi!"));

        // Gelen ve giden işlemleri birlikte getir
        return transactionRepository.findAllByFromWalletIdOrToWalletId(wallet.getId(), wallet.getId(), pageable);
    }
}

