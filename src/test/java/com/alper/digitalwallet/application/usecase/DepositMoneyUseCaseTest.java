package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.IdempotencyConflictException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.IdempotencyKeyStatus;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import com.alper.digitalwallet.domain.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepositMoneyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private DepositMoneyUseCase depositMoneyUseCase;

    @BeforeEach
    void setUp() {
        depositMoneyUseCase = spy(new DepositMoneyUseCase(
                walletRepository, transactionRepository, idempotencyService, idempotencyKeyRepository));
    }

    @Test
    void execute_successfulDeposit() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        when(transactionRepository.save(any())).thenReturn(null);

        Wallet result = depositMoneyUseCase.execute(1L, new BigDecimal("50.00"));

        assertEquals(new BigDecimal("150.00"), result.getBalance());
        verify(walletRepository).findByUserId(1L);
        verify(walletRepository).save(wallet);
        verify(transactionRepository).save(any());
    }

    @Test
    void execute_invalidAmount() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(InvalidAmountException.class, () ->
                depositMoneyUseCase.execute(1L, new BigDecimal("-1.00"))
        );

        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_zeroAmount() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(InvalidAmountException.class, () ->
                depositMoneyUseCase.execute(1L, BigDecimal.ZERO)
        );

        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_nullAmount() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        assertThrows(InvalidAmountException.class, () ->
                depositMoneyUseCase.execute(1L, null)
        );

        verify(walletRepository).findByUserId(1L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_walletNotFound() {
        when(walletRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                depositMoneyUseCase.execute(999L, new BigDecimal("20.00"))
        );

        verify(walletRepository).findByUserId(999L);
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_idempotencyKey_returnsCachedResult_whenCompleted() {
        String idempotencyKey = "duplicate-key-123";
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("150.00"))
                .currency("TRY")
                .build();
        Transaction transaction = Transaction.builder()
                .id(100L)
                .toWalletId(1L)
                .amount(new BigDecimal("50.00"))
                .build();
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .key(idempotencyKey)
                .status(IdempotencyKeyStatus.COMPLETED)
                .transactionId(100L)
                .build();

        when(idempotencyService.tryCreateIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(idempotencyKeyRepository.findByKey(idempotencyKey)).thenReturn(Optional.of(existingKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any())).thenReturn(wallet);

        Wallet result = depositMoneyUseCase.execute(1L, new BigDecimal("50.00"), idempotencyKey);

        assertEquals(new BigDecimal("150.00"), result.getBalance());
        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_idempotencyKey_throws_whenInProgress() {
        String idempotencyKey = "in-progress-key-123";
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .key(idempotencyKey)
                .status(IdempotencyKeyStatus.IN_PROGRESS)
                .build();

        when(idempotencyService.tryCreateIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(idempotencyKeyRepository.findByKey(idempotencyKey)).thenReturn(Optional.of(existingKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any()))
                .thenThrow(new IdempotencyConflictException("Ayni idempotency key ile islem devam ediyor. Lutfen bekleyin."));

        assertThrows(IdempotencyConflictException.class, () ->
                depositMoneyUseCase.execute(1L, new BigDecimal("50.00"), idempotencyKey)
        );

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void execute_idempotencyKey_throws_whenFailed() {
        String idempotencyKey = "failed-key-123";
        IdempotencyKey existingKey = IdempotencyKey.builder()
                .key(idempotencyKey)
                .status(IdempotencyKeyStatus.FAILED)
                .build();

        when(idempotencyService.tryCreateIdempotencyKey(idempotencyKey)).thenReturn(false);
        when(idempotencyKeyRepository.findByKey(idempotencyKey)).thenReturn(Optional.of(existingKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any()))
                .thenThrow(new IdempotencyConflictException("Onceki islem basarisiz oldu. Lutfen yeni bir idempotency key ile deneyin."));

        assertThrows(IdempotencyConflictException.class, () ->
                depositMoneyUseCase.execute(1L, new BigDecimal("50.00"), idempotencyKey)
        );

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
