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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
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

import java.util.Set;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Cuzdan yonetimi ve para transfer islemi")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("transactionDate", "amount", "id");

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
        @ApiResponse(responseCode = "400", description = "Gecersiz istek"),
        @ApiResponse(responseCode = "403", description = "Erisim reddedildi")
    })
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request, Authentication authentication) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        assertUserAccess(authenticatedUserId, request.getUserId());
        Wallet wallet = createWalletUseCase.execute(request.getUserId(), request.getCurrency());
        return new ResponseEntity<>(mapToResponse(wallet), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Cuzdan bilgisini getir", description = "Kullanici ID'ye gore cuzdan bilgisini alir")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuzdan bilgisi"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi")
    })
    public ResponseEntity<WalletResponse> getWallet(@RequestParam(required = false) Long userId, Authentication authentication) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        assertUserAccess(authenticatedUserId, userId);
        Wallet wallet = getWalletUseCase.execute(authenticatedUserId);
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @GetMapping("/{walletId}")
    @Operation(summary = "Cuzdan bilgisini ID ile getir")
    public ResponseEntity<WalletResponse> getWalletById(@PathVariable Long walletId, Authentication authentication) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        Wallet wallet = getWalletUseCase.executeById(walletId);
        assertUserAccess(authenticatedUserId, wallet.getUserId());
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Para yatir", description = "Cuzdana para yatirir")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Para basariyla yatirildi"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi"),
        @ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    public ResponseEntity<WalletResponse> deposit(
            @Valid @RequestBody DepositRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false)
            @Parameter(description = "Para yatirma icin idempotency anahtari") String idempotencyKey) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        assertUserAccess(authenticatedUserId, request.getUserId());
        Wallet wallet = depositMoneyUseCase.execute(authenticatedUserId, request.getAmount(), idempotencyKey);
        return ResponseEntity.ok(mapToResponse(wallet));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Para cek", description = "Cuzdandan para cekmek")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Para basariyla cekildi"),
        @ApiResponse(responseCode = "400", description = "Yetersiz bakiye"),
        @ApiResponse(responseCode = "404", description = "Cuzdan bulunamadi"),
        @ApiResponse(responseCode = "409", description = "Idempotency conflict")
    })
    public ResponseEntity<WalletResponse> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false)
            @Parameter(description = "Para cekme icin idempotency anahtari") String idempotencyKey) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        assertUserAccess(authenticatedUserId, request.getUserId());
        Wallet wallet = withdrawMoneyUseCase.execute(authenticatedUserId, request.getAmount(), idempotencyKey);
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
            Authentication authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) 
            @Parameter(description = "Transfer için idempotency anahtarı") String idempotencyKey) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        Transaction transaction = transferMoneyUseCase.execute(
                authenticatedUserId,
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
            @Parameter(description = "Sıralama yönü") @RequestParam(defaultValue = "DESC") String direction,
            Authentication authentication) {
        Long authenticatedUserId = getAuthenticatedUserId(authentication);
        Wallet wallet = getWalletUseCase.executeById(walletId);
        assertUserAccess(authenticatedUserId, wallet.getUserId());

        validateSortField(sortBy);
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        Pageable pageable = PageRequest.of(page, size, sortDirection, sortBy);
        Page<Transaction> transactions = getTransactionsUseCase.execute(wallet.getId(), pageable);
        
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

    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AccessDeniedException("Kimlik dogrulamasi gerekli");
        }
        try {
            return Long.parseLong(authentication.getPrincipal().toString());
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("Gecersiz kimlik bilgisi");
        }
    }

    private void assertUserAccess(Long authenticatedUserId, Long requestedUserId) {
        if (requestedUserId != null && !authenticatedUserId.equals(requestedUserId)) {
            throw new AccessDeniedException("Baska bir kullanici adina islem yapamazsiniz");
        }
    }

    private void validateSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Gecersiz sortBy alani: " + sortBy);
        }
    }
}
