package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetWalletUseCase {

    private final WalletRepository walletRepository;

    public Wallet execute(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException("Kullaniciya ait cuzdan bulunamadi!"));
    }

    public Wallet executeById(Long walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Cuzdan bulunamadi!"));
    }
}

