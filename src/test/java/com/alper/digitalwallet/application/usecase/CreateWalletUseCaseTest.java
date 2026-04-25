package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.WalletAlreadyExistsException;
import com.alper.digitalwallet.domain.model.Wallet;
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
class CreateWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private CreateWalletUseCase createWalletUseCase;

    @Test
    void execute_success() {
        Wallet wallet = Wallet.builder()
                .id(1L)
                .userId(10L)
                .balance(BigDecimal.ZERO)
                .currency("TRY")
                .build();

        when(walletRepository.findByUserId(10L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        Wallet result = createWalletUseCase.execute(10L, "TRY");

        assertEquals(10L, result.getUserId());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertEquals("TRY", result.getCurrency());
        verify(walletRepository).findByUserId(10L);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void execute_duplicateWallet() {
        when(walletRepository.findByUserId(10L))
                .thenReturn(Optional.of(Wallet.builder().id(1L).userId(10L).build()));

        assertThrows(WalletAlreadyExistsException.class, () -> createWalletUseCase.execute(10L, "TRY"));

        verify(walletRepository).findByUserId(10L);
        verify(walletRepository, never()).save(any(Wallet.class));
    }
}

