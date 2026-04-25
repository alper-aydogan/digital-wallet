package com.alper.digitalwallet.application.usecase;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionsUseCaseTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetTransactionsUseCase getTransactionsUseCase;

    @Test
    void execute_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Wallet wallet = Wallet.builder().id(1L).userId(10L).currency("TRY").build();
        Transaction transaction = Transaction.builder()
                .id(1L)
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(new BigDecimal("25.00"))
                .build();
        Page<Transaction> expectedPage = new PageImpl<>(List.of(transaction), pageable, 1);

        when(walletRepository.findById(1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findAllByFromWalletIdOrToWalletId(1L, pageable)).thenReturn(expectedPage);

        Page<Transaction> result = getTransactionsUseCase.execute(1L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(transaction, result.getContent().get(0));
        verify(walletRepository).findById(1L);
        verify(transactionRepository).findAllByFromWalletIdOrToWalletId(1L, pageable);
    }

    @Test
    void execute_walletNotFound() {
        Pageable pageable = PageRequest.of(0, 10);
        when(walletRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> getTransactionsUseCase.execute(999L, pageable));

        verify(walletRepository).findById(999L);
        verify(transactionRepository, never()).findAllByFromWalletIdOrToWalletId(999L, pageable);
    }

    @Test
    void execute_emptyResult() {
        Pageable pageable = PageRequest.of(0, 10);
        Wallet wallet = Wallet.builder().id(2L).userId(20L).currency("TRY").build();
        Page<Transaction> emptyPage = Page.empty(pageable);

        when(walletRepository.findById(2L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findAllByFromWalletIdOrToWalletId(2L, pageable)).thenReturn(emptyPage);

        Page<Transaction> result = getTransactionsUseCase.execute(2L, pageable);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        verify(walletRepository).findById(2L);
        verify(transactionRepository).findAllByFromWalletIdOrToWalletId(2L, pageable);
    }
}

