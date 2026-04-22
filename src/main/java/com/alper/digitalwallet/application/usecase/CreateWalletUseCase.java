package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.exception.WalletAlreadyExistsException;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreateWalletUseCase {

    private final WalletRepository walletRepository;

    @Transactional
    public Wallet execute(Long userId, String currency) {
        // Duplicate kontrolü: aynı userId için cüzdan varsa hata fırlat
        if (walletRepository.findByUserId(userId).isPresent()) {
            throw new WalletAlreadyExistsException("Bu kullanici icin zaten bir cuzdan var!");
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .build();

        return walletRepository.save(wallet);
    }
}

