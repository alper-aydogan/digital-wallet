package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DepositMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Wallet execute(Long userId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new RuntimeException("Yatirilacak tutar sifirdan buyuk olmalidir!");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Kullaniciya ait cuzdan bulunamadi!"));

        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet updatedWallet = walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .toWalletId(updatedWallet.getId())
                .amount(amount)
                .transactionDate(LocalDateTime.now())
                .description("Para Yatirma Islemi")
                .build();
        transactionRepository.save(transaction);

        return updatedWallet;
    }
}

