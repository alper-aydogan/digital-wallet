package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.application.dto.request.CreateWalletRequest;
import com.alper.digitalwallet.application.dto.request.DepositRequest;
import com.alper.digitalwallet.application.dto.request.TransferRequest;
import com.alper.digitalwallet.application.dto.request.WithdrawRequest;
import com.alper.digitalwallet.application.dto.response.TransactionResponse;
import com.alper.digitalwallet.application.dto.response.WalletResponse;
import com.alper.digitalwallet.application.usecase.CreateWalletUseCase;
import com.alper.digitalwallet.application.usecase.DepositMoneyUseCase;
import com.alper.digitalwallet.application.usecase.GetTransactionsUseCase;
import com.alper.digitalwallet.application.usecase.GetWalletUseCase;
import com.alper.digitalwallet.application.usecase.TransferMoneyUseCase;
import com.alper.digitalwallet.application.usecase.WithdrawMoneyUseCase;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Cuzdan yonetimi ve para transfer islemi")
public class WalletController {

    private final CreateWalletUseCase createWalletUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;
    private final WithdrawMoneyUseCase withdrawMoneyUseCase;
    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetWalletUseCase getWalletUseCase;
    private final GetTransactionsUseCase getTransactionsUseCase;

    @PostMapping
    @Operation(summary = "Yeni cuzdan olustur", description = "Kullanici icin yeni bir dijital cuzdan olusturur")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cuzdan basariyla olusturuldu"),
        @ApiResponse(responseCode = "400", description = "Gecersiz istek")
    })
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        Wallet wallet = createWalletUseCase.execute(request.getUserId(), request.getCurrency());
        return new ResponseEntity<>(mapToResponse(wallet), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Cuzdan bilgisini getir", description = "Kullanici ID'ye gore cuzdan bilgisini alir")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuzdan bilgisi"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi")
    })
    public ResponseEntity<WalletResponse> getWallet(@RequestParam Long userId) {
        Wallet wallet = getWalletUseCase.execute(userId);
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Cuzdan bilgisini ID ile getir")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable Long walletId) {
        Wallet wallet = getWalletUseCase.executeById(walletId);
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Para yatir", description = "Cuzdana para yatirir")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Para basariyla yatirildi"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi")
    })
    public ResponseEntity<WalletResponse> deposit(@Valid @RequestBody DepositRequest request) {
        Wallet wallet = depositMoneyUseCase.execute(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Para cek", description = "Cuzdandan para cekmek")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Para basariyla cekildi"),
        @ApiResponse(responseCode = "400", description = "Yetersiz bakiye"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi")
    })
    public ResponseEntity<WalletResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Wallet wallet = withdrawMoneyUseCase.execute(request.getUserId(), request.getAmount());
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Para transfer et", description = "Bir cuzdandan digerine para transferi yapar")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer basarili"),
        @ApiResponse(responseCode = "400", description = "Transfer edilemedi"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi")
    })
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) 
            @Parameter(description = "Transfer için idempotency anahtarı") String idempotencyKey) {
        Transaction transaction = transferMoneyUseCase.execute(
                request.getFromUserId(),
                request.getToUserId(),
                request.getAmount(),
                idempotencyKey
        );
        return new ResponseEntity<>(mapTransactionToResponse(transaction), HttpStatus.CREATED);
    }

    @GetMapping("/{walletId}/transactions")
    @Operation(summary = "İşlem geçmişini getir", description = "Cüzdan için işlem geçmişini sayfalama ile döner")
    @ApiResponse(responseCode = "200", description = "İşlem geçmişi")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @PathVariable Long walletId,
            @Parameter(description = "Sayfa numarası (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Sayfa boyutu") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sıralama alanı") @RequestParam(defaultValue = "transactionDate") String sortBy,
            @Parameter(description = "Sıralama yönü") @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        
        Pageable pageable = PageRequest.of(page, size, direction, sortBy);
        Page<Transaction> transactions = getTransactionsUseCase.execute(walletId, pageable);
        
        return ResponseEntity.ok(transactions.map(this::mapTransactionToResponse));
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    private TransactionResponse mapTransactionToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .fromWalletId(transaction.getFromWalletId())
                .toWalletId(transaction.getToWalletId())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .build();
    }
}

