package com.alper.digitalwallet.domain.service;

import com.alper.digitalwallet.domain.exception.IdempotencyConflictException;
import com.alper.digitalwallet.domain.exception.IdempotencyPayloadMismatchException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.IdempotencyKeyStatus;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

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
            return false;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeIdempotencyKey(String key, Long transactionId) {
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findByKey(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency key not found: " + key));
        idempotencyKey.complete(transactionId);
        idempotencyKeyRepository.save(idempotencyKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markIdempotencyKeyFailed(String key) {
        IdempotencyKey idempotencyKey = idempotencyKeyRepository.findByKey(key)
                .orElse(null);
        if (idempotencyKey != null && idempotencyKey.getStatus() == IdempotencyKeyStatus.IN_PROGRESS) {
            idempotencyKey.markFailed();
            idempotencyKeyRepository.save(idempotencyKey);
        }
    }

    public <T> T handleExistingIdempotencyKey(IdempotencyKey existingKey, Function<String, Optional<T>> transactionFinder) {
        switch (existingKey.getStatus()) {
            case COMPLETED:
                if (existingKey.getTransactionId() != null) {
                    return transactionFinder.apply(existingKey.getKey())
                            .orElseThrow(() -> new IllegalStateException("Completed transaction not found"));
                }
                throw new IllegalStateException("Completed idempotency key without transaction reference");

            case IN_PROGRESS:
                throw new IdempotencyConflictException("Ayni idempotency key ile islem devam ediyor. Lutfen bekleyin.");

            case FAILED:
                throw new IdempotencyConflictException("Onceki islem basarisiz oldu. Lutfen yeni bir idempotency key ile deneyin.");

            default:
                throw new IllegalStateException("Bilinmeyen idempotency status: " + existingKey.getStatus());
        }
    }

    public Transaction handleExistingIdempotencyKeyWithPayloadCheck(IdempotencyKey existingKey, Function<String, Optional<Transaction>> transactionFinder, Long expectedFromWalletId, Long expectedToWalletId, BigDecimal expectedAmount) {
        switch (existingKey.getStatus()) {
            case COMPLETED:
                if (existingKey.getTransactionId() != null) {
                    Transaction transaction = transactionFinder.apply(existingKey.getKey())
                            .orElseThrow(() -> new IllegalStateException("Completed transaction not found"));
                    
                    // Payload doğrulama: expected değerler verildiyse karşılaştır
                    if (expectedFromWalletId != null && expectedToWalletId != null && expectedAmount != null) {
                        boolean fromWalletMatches = expectedFromWalletId.equals(transaction.getFromWalletId()) ||
                                (transaction.getFromWalletId() == null && expectedFromWalletId.equals(transaction.getToWalletId()));
                        boolean toWalletMatches = expectedToWalletId.equals(transaction.getToWalletId());
                        boolean amountMatches = expectedAmount.compareTo(transaction.getAmount()) == 0;
                        
                        // Deposit/Withdraw durumları için fromWallet null olabilir
                        if (transaction.getFromWalletId() == null) {
                            fromWalletMatches = true; // Deposit durumu
                        }
                        
                        if (!fromWalletMatches || !toWalletMatches || !amountMatches) {
                            throw new IdempotencyPayloadMismatchException(
                                    "Ayni idempotency key ile farkli parametreler gonderildi. " +
                                    "Beklenen: fromWallet=" + transaction.getFromWalletId() + 
                                    ", toWallet=" + transaction.getToWalletId() + 
                                    ", amount=" + transaction.getAmount());
                        }
                    }
                    
                    return transaction;
                }
                throw new IllegalStateException("Completed idempotency key without transaction reference");

            case IN_PROGRESS:
                throw new IdempotencyConflictException("Ayni idempotency key ile islem devam ediyor. Lutfen bekleyin.");

            case FAILED:
                throw new IdempotencyConflictException("Onceki islem basarisiz oldu. Lutfen yeni bir idempotency key ile deneyin.");

            default:
                throw new IllegalStateException("Bilinmeyen idempotency status: " + existingKey.getStatus());
        }
    }
}
