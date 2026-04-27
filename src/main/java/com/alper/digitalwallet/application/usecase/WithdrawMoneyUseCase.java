package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import com.alper.digitalwallet.domain.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WithdrawMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public Wallet execute(Long userId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            boolean created = idempotencyService.tryCreateIdempotencyKey(idempotencyKey);

            if (!created) {
                IdempotencyKey existingKey = idempotencyKeyRepository.findByKey(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Idempotency key not found after conflict"));

                return idempotencyService.handleExistingIdempotencyKey(existingKey,
                        key -> transactionRepository.findByIdempotencyKey(key)
                                .map(t -> walletRepository.findById(t.getFromWalletId()).orElse(null)));
            }
        }

        try {
            Wallet wallet = performWithdraw(userId, amount, idempotencyKey);

            if (idempotencyKey != null) {
                Transaction transaction = transactionRepository.findByIdempotencyKey(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Transaction not found after withdraw"));
                idempotencyService.completeIdempotencyKey(idempotencyKey, transaction.getId());
            }

            return wallet;
        } catch (Exception e) {
            if (idempotencyKey != null) {
                idempotencyService.markIdempotencyKeyFailed(idempotencyKey);
            }
            throw e;
        }
    }

    private Wallet performWithdraw(Long userId, BigDecimal amount, String idempotencyKey) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Kullaniciya ait cuzdan bulunamadi!"));

        wallet.debit(amount);
        Wallet updatedWallet = walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .fromWalletId(updatedWallet.getId())
                .amount(amount)
                .transactionDate(LocalDateTime.now())
                .description("Para Cekme Islemi")
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepository.save(transaction);

        return updatedWallet;
    }

    @Transactional
    public Wallet execute(Long userId, BigDecimal amount) {
        return execute(userId, amount, null);
    }
}

