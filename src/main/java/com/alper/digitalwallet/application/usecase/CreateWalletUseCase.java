package com.alper.digitalwallet.application.usecase;

import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CreateWalletUseCase {

    private final WalletRepository walletRepository;

    public Wallet execute(Long userId, String currency) {
        // Eğer kullanıcının zaten cüzdanı varsa hata fırlatabilirsin (Opsiyonel)

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .currency(currency)
                .build();

        return walletRepository.save(wallet);
    }
}

