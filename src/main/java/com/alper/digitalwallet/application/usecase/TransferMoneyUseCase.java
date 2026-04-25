package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.InsufficientBalanceException;
import com.alper.digitalwallet.domain.exception.InvalidCurrencyException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferMoneyUseCase {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount, String idempotencyKey) {
        // Idempotency kontrolü: aynı key varsa cached sonucu dön
        if (idempotencyKey != null) {
            var existingTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
            if (existingTx.isPresent()) {
                return existingTx.get();
            }
        }

        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Transfer tutari sifirdan buyuk olmalidir!");
        }

        if (fromUserId.equals(toUserId)) {
            throw new InvalidAmountException("Ayni kullaniciya transfer yapilamaz!");
        }

        Wallet fromWallet = walletRepository.findByUserIdWithLock(fromUserId)
                .orElseThrow(() -> new WalletNotFoundException("Gonderen cuzdan bulunamadi!"));

        Wallet toWallet = walletRepository.findByUserIdWithLock(toUserId)
                .orElseThrow(() -> new WalletNotFoundException("Alan cuzdan bulunamadi!"));

        if (!fromWallet.getCurrency().equals(toWallet.getCurrency())) {
            throw new InvalidCurrencyException("Transfer icin para birimleri ayni olmalidir!");
        }

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Yetersiz bakiye! Mevcut bakiye: " + fromWallet.getBalance());
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(amount));
        toWallet.setBalance(toWallet.getBalance().add(amount));

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

        try {
            return transactionRepository.save(transaction);
        } catch (DataIntegrityViolationException e) {
            // Idempotency key zaten varsa, aynı işlemi yeniden oku ve dön
            if (idempotencyKey != null) {
                var cachedTx = transactionRepository.findByIdempotencyKey(idempotencyKey);
                if (cachedTx.isPresent()) {
                    return cachedTx.get();
                }
            }
            throw e;
        }
    }

    @Transactional
    public Transaction execute(Long fromUserId, Long toUserId, BigDecimal amount) {
        return execute(fromUserId, toUserId, amount, null);
    }
}
