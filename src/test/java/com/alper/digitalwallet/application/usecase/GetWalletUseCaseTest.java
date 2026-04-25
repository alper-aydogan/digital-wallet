package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetWalletUseCase getWalletUseCase;

    @Test
    void execute_successByUserId() {
        Wallet wallet = Wallet.builder().id(1L).userId(10L).currency("TRY").build();
        when(walletRepository.findByUserId(10L)).thenReturn(Optional.of(wallet));

        Wallet result = getWalletUseCase.execute(10L);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getUserId());
        verify(walletRepository).findByUserId(10L);
    }

    @Test
    void execute_userWalletNotFound() {
        when(walletRepository.findByUserId(99L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> getWalletUseCase.execute(99L));
        verify(walletRepository).findByUserId(99L);
    }

    @Test
    void executeById_success() {
        Wallet wallet = Wallet.builder().id(2L).userId(20L).currency("TRY").build();
        when(walletRepository.findById(2L)).thenReturn(Optional.of(wallet));

        Wallet result = getWalletUseCase.executeById(2L);

        assertEquals(2L, result.getId());
        assertEquals(20L, result.getUserId());
        verify(walletRepository).findById(2L);
    }

    @Test
    void executeById_walletNotFound() {
        when(walletRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> getWalletUseCase.executeById(404L));
        verify(walletRepository).findById(404L);
    }
}

