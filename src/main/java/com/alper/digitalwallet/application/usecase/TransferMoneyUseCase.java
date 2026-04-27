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

                // Cüzdan ID'lerini bul (payload doğrulama için)
                Wallet fromWalletForCheck = walletRepository.findByUserId(fromUserId)
                        .orElseThrow(() -> new WalletNotFoundException("Gonderen cuzdan bulunamadi!"));
                Wallet toWalletForCheck = walletRepository.findByUserId(toUserId)
                        .orElseThrow(() -> new WalletNotFoundException("Alan cuzdan bulunamadi!"));

                return idempotencyService.handleExistingIdempotencyKeyWithPayloadCheck(existingKey,
                        key -> transactionRepository.findByIdempotencyKey(key),
                        fromWalletForCheck.getId(),
                        toWalletForCheck.getId(),
                        amount);
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

        // Önce cüzdanları kilitsiz olarak bul
        Wallet fromWalletUnlocked = walletRepository.findByUserId(fromUserId)
                .orElseThrow(() -> new WalletNotFoundException("Gonderen cuzdan bulunamadi!"));
        Wallet toWalletUnlocked = walletRepository.findByUserId(toUserId)
                .orElseThrow(() -> new WalletNotFoundException("Alan cuzdan bulunamadi!"));

        Long fromWalletId = fromWalletUnlocked.getId();
        Long toWalletId = toWalletUnlocked.getId();

        // Deterministik lock sırası: küçük walletId önce (deadlock önlemi)
        Long firstWalletId = Math.min(fromWalletId, toWalletId);
        Long secondWalletId = Math.max(fromWalletId, toWalletId);

        Wallet firstWallet = walletRepository.findByIdWithLock(firstWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Cuzdan bulunamadi!"));

        Wallet secondWallet = walletRepository.findByIdWithLock(secondWalletId)
                .orElseThrow(() -> new WalletNotFoundException("Cuzdan bulunamadi!"));

        // İş kuralı: fromWallet bakiyesi azalır, toWallet artar
        Wallet fromWallet = fromWalletId.equals(firstWalletId) ? firstWallet : secondWallet;
        Wallet toWallet = toWalletId.equals(firstWalletId) ? firstWallet : secondWallet;

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
