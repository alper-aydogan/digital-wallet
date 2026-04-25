package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.IdempotencyConflictException;
import com.alper.digitalwallet.domain.exception.InvalidCurrencyException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.IdempotencyKeyStatus;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount, String idempotencyKey) {
        if (idempotencyKey != null) {
            // Try to create idempotency record atomically
            boolean created = tryCreateIdempotencyKey(idempotencyKey);

            if (!created) {
                // Key already exists - check status
                IdempotencyKey existingKey = idempotencyKeyRepository.findByKey(idempotencyKey)
                        .orElseThrow(() -> new IllegalStateException("Idempotency key not found after conflict"));

                return handleExistingIdempotencyKey(existingKey);
            }
        }

        try {
            Transaction transaction = performTransfer(fromUserId, toUserId, amount, idempotencyKey);

            // Mark idempotency key as completed
            if (idempotencyKey != null) {
                completeIdempotencyKey(idempotencyKey, transaction.getId());
            }

            return transaction;
        } catch (Exception e) {
            // Mark idempotency key as failed on error
            if (idempotencyKey != null) {
                markIdempotencyKeyFailed(idempotencyKey);
            }
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryCreateIdempotencyKey(String key) {
        try {
            IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                    .key(key)
                    .status(IdempotencyKeyStatus.IN_PROGRESS)
                    .build();
            idempotencyKeyRepository.save(idempotencyKey);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Unique constraint violation - key already exists
            return false;
        }
    }

    private Transaction handleExistingIdempotencyKey(IdempotencyKey existingKey) {
        switch (existingKey.getStatus()) {
            case COMPLETED:
                // Return the cached transaction
                if (existingKey.getTransactionId() != null) {
                    return transactionRepository.findByIdempotencyKey(existingKey.getKey())
                            .orElseThrow(() -> new IllegalStateException("Completed transaction not found"));
                }
                throw new IllegalStateException("Completed idempotency key without transaction reference");

            case IN_PROGRESS:
                // Concurrent request in progress
                throw new IdempotencyConflictException("Ayni idempotency key ile islem devam ediyor. Lutfen bekleyin.");

            case FAILED:
                // Previous attempt failed - client should retry with new key
                throw new IdempotencyConflictException("Onceki islem basarisiz oldu. Lutfen yeni bir idempotency key ile deneyin.");

            default:
                throw new IllegalStateException("Bilinmeyen idempotency status: " + existingKey.getStatus());
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
                .description("Para Transferi")
                .idempotencyKey(idempotencyKey)
                .build();

        return transactionRepository.save(transaction);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeIdempotencyKey(String key, Long transactionId) {
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findByKey(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key not found: " + key));
        idempotencyKey.setStatus(IdempotencyKeyStatus.COMPLETED);
        idempotencyKey.setTransactionId(transactionId);
        idempotencyKeyRepository.save(idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIdempotencyKeyFailed(String key) {
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findByKey(key)
                .orElse(null);
        if (idempotencyKey != null && idempotencyKey.getStatus() == IdempotencyKeyStatus.IN_PROGRESS) {
            idempotencyKey.setStatus(IdempotencyKeyStatus.FAILED);
            idempotencyKeyRepository.save(idempotencyKey);
        }
    }

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount) {
        return execute(fromUserId, toUserId, amount, null);
    }
}
