package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.application.dto.request.CreateWalletRequest;
import com.alper.digitalwallet.application.dto.request.DepositRequest;
import com.alper.digitalwallet.application.dto.response.WalletResponse;
import com.alper.digitalwallet.application.usecase.CreateWalletUseCase;
import com.alper.digitalwallet.application.usecase.DepositMoneyUseCase;
import com.alper.digitalwallet.domain.model.Wallet;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = createWalletUseCase.execute(request.getUserId(), request.getCurrency());
        return new ResponseEntity<>(mapToResponse(wallet), HttpStatus.CREATED);
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(@Valid @RequestBody DepositRequest request) {
        Wallet wallet = depositMoneyUseCase.execute(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }
}

