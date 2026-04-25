package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.InsufficientBalanceException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.InvalidCurrencyException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferMoneyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransferMoneyUseCase transferMoneyUseCase;

    @Test
    void execute_successfulTransfer() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"));

        assertNotNull(result);
        assertEquals(1L, result.getFromWalletId());
        assertEquals(2L, result.getToWalletId());
        assertEquals(new BigDecimal("30.00"), result.getAmount());
    }

    @Test
    void execute_samUserTransfer() {
        assertThrows(InvalidAmountException.class, () ->
                transferMoneyUseCase.execute(1L, 1L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_nullAmount() {
        assertThrows(InvalidAmountException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, null)
        );

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void execute_insufficientBalance() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("20.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(toWallet));

        assertThrows(InsufficientBalanceException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_invalidCurrency() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(toWallet));

        assertThrows(InvalidCurrencyException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_fromWalletNotFound() {
        when(walletRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                transferMoneyUseCase.execute(999L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_toWalletNotFound() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                transferMoneyUseCase.execute(1L, 999L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_idempotencyKey_returnsCachedResult() {
        Transaction cachedTransaction = Transaction.builder()
                .id(1L)
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(new BigDecimal("30.00"))
                .build();

        when(transactionRepository.findByIdempotencyKey("idempotency-123"))
                .thenReturn(Optional.of(cachedTransaction));

        Transaction result = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"), "idempotency-123");

        assertEquals(cachedTransaction, result);
    }
}
