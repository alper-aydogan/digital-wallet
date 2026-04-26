package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.InvalidCurrencyException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.TransactionType;
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
public class TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            boolean created = idempotencyService.tryCreateIdempotencyKey(idempotencyKey);

            if (!created) {
                IdempotencyKey existingKey = idempotencyKeyRepository.findByKey(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Idempotency key not found after conflict"));

                return idempotencyService.handleExistingIdempotencyKey(existingKey,
                        key -> transactionRepository.findByIdempotencyKey(key));
            }
        }

        try {
            Transaction transaction = performTransfer(fromUserId, toUserId, amount, idempotencyKey);

            if (idempotencyKey != null) {
                idempotencyService.completeIdempotencyKey(idempotencyKey, transaction.getId());
            }

            return transaction;
        } catch (Exception e) {
            if (idempotencyKey != null) {
                idempotencyService.markIdempotencyKeyFailed(idempotencyKey);
            }
            throw e;
        }
    }


    private Transaction performTransfer(Long fromUserId, Long toUserId, BigDecimal amount, String idempotencyKey) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Transfer tutari sifirdan buyuk olmalidir!");
        }

        if (fromUserId.equals(toUserId)) {
            throw new InvalidAmountException("Ayni kullaniciya transfer yapilamaz!");
        }

        // Deterministik lock sırası: küçük userId önce (deadlock önlemi)
        Long firstUserId = Math.min(fromUserId, toUserId);
        Long secondUserId = Math.max(fromUserId, toUserId);

        Wallet firstWallet = walletRepository.findByUserIdWithLock(firstUserId)
                .orElseThrow(() -> new WalletNotFoundException(
                        firstUserId.equals(fromUserId) ? "Gonderen cuzdan bulunamadi!" : "Alan cuzdan bulunamadi!"));

        Wallet secondWallet = walletRepository.findByUserIdWithLock(secondUserId)
                .orElseThrow(() -> new WalletNotFoundException(
                        secondUserId.equals(fromUserId) ? "Gonderen cuzdan bulunamadi!" : "Alan cuzdan bulunamadi!"));

        // İş kuralı: fromUser bakiyesi azalır, toUser artar
        Wallet fromWallet = fromUserId.equals(firstUserId) ? firstWallet : secondWallet;
        Wallet toWallet = toUserId.equals(firstUserId) ? firstWallet : secondWallet;

        if (!fromWallet.isSameCurrencyAs(toWallet)) {
            throw new InvalidCurrencyException("Transfer icin para birimleri ayni olmalidir!");
        }

        fromWallet.debit(amount);
        toWallet.credit(amount);

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        Transaction transaction = Transaction.builder()
                .fromWalletId(fromWallet.getId())
                .toWalletId(toWallet.getId())
                .amount(amount)
                .transactionDate(LocalDateTime.now())
                .type(TransactionType.TRANSFER)
                .idempotencyKey(idempotencyKey)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount) {
        return execute(fromUserId, toUserId, amount, null);
    }
}
