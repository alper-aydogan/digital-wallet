package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositMoneyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DepositMoneyUseCase depositMoneyUseCase;

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
}
