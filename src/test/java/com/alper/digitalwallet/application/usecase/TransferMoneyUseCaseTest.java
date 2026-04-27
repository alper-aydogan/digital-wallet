package com.alper.digitalwallet.application.usecase;
import com.alper.digitalwallet.domain.exception.IdempotencyConflictException;
import com.alper.digitalwallet.domain.exception.IdempotencyPayloadMismatchException;
import com.alper.digitalwallet.domain.exception.InsufficientBalanceException;
import com.alper.digitalwallet.domain.exception.InvalidAmountException;
import com.alper.digitalwallet.domain.exception.InvalidCurrencyException;
import com.alper.digitalwallet.domain.exception.WalletNotFoundException;
import com.alper.digitalwallet.domain.model.IdempotencyKey;
import com.alper.digitalwallet.domain.model.IdempotencyKeyStatus;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.IdempotencyKeyRepository;
import com.alper.digitalwallet.domain.repository.TransactionRepository;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import com.alper.digitalwallet.domain.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferMoneyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private TransferMoneyUseCase transferMoneyUseCase;

    @BeforeEach
    void setUp() {
        transferMoneyUseCase = spy(new TransferMoneyUseCase(
                walletRepository, transactionRepository, idempotencyService, idempotencyKeyRepository));
    }

    @Test
    void execute_successfulTransfer() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        Transaction result = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"));

        assertNotNull(result);
        assertEquals(1L, result.getFromWalletId());
        assertEquals(2L, result.getToWalletId());
        assertEquals(new BigDecimal("30.00"), result.getAmount());
        assertEquals(new BigDecimal("70.00"), fromWallet.getBalance());
        assertEquals(new BigDecimal("80.00"), toWallet.getBalance());
    }

    @Test
    void execute_samUserTransfer() {
        assertThrows(InvalidAmountException.class, () ->
                transferMoneyUseCase.execute(1L, 1L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_nullAmount() {
        assertThrows(InvalidAmountException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, null)
        );

        verify(walletRepository, never()).findByUserId(any());
        verify(walletRepository, never()).save(any(Wallet.class));
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void execute_insufficientBalance() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("20.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(toWallet));

        assertThrows(InsufficientBalanceException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_invalidCurrency() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("USD")
                .build();

        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(toWallet));

        assertThrows(InvalidCurrencyException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_fromWalletNotFound() {
        // fromUserId (999) > toUserId (2) olduğu için sıra: 2 -> 999
        // İlk çağrı 2L için, ikincisi 999L için
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                transferMoneyUseCase.execute(999L, 2L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_toWalletNotFound() {
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        // fromUserId (1) < toUserId (999) olduğu için sıra: 1 -> 999
        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(999L)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () ->
                transferMoneyUseCase.execute(1L, 999L, new BigDecimal("50.00"))
        );
    }

    @Test
    void execute_idempotencyKey_returnsCachedResult_whenCompleted() {
        // Given: Idempotency key exists with COMPLETED status
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .id(1L)
                .key("idempotency-123")
                .status(IdempotencyKeyStatus.COMPLETED)
                .transactionId(100L)
                .build();

        Transaction completedTransaction = Transaction.builder()
                .id(100L)
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(new BigDecimal("30.00"))
                .idempotencyKey("idempotency-123")
                .build();

        // Simulate key already exists
        when(idempotencyService.tryCreateIdempotencyKey("idempotency-123")).thenReturn(false);
        when(idempotencyKeyRepository.findByKey("idempotency-123")).thenReturn(Optional.of(idempotencyKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any())).thenReturn(completedTransaction);

        // When
        Transaction result = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"), "idempotency-123");

        // Then: Return cached result without modifying wallets
        assertEquals(completedTransaction, result);
        verify(walletRepository, never()).findByUserIdWithLock(any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void execute_idempotencyKey_throwsConflict_whenInProgress() {
        // Given: Idempotency key exists with IN_PROGRESS status
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .id(1L)
                .key("idempotency-123")
                .status(IdempotencyKeyStatus.IN_PROGRESS)
                .build();

        // Simulate key already exists
        when(idempotencyService.tryCreateIdempotencyKey("idempotency-123")).thenReturn(false);
        when(idempotencyKeyRepository.findByKey("idempotency-123")).thenReturn(Optional.of(idempotencyKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any()))
                .thenThrow(new IdempotencyConflictException("Ayni idempotency key ile islem devam ediyor. Lutfen bekleyin."));

        // When/Then
        IdempotencyConflictException exception = assertThrows(IdempotencyConflictException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"), "idempotency-123")
        );

        assertTrue(exception.getMessage().contains("islem devam ediyor"));
        verify(walletRepository, never()).findByUserIdWithLock(any());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void execute_idempotencyKey_throwsConflict_whenFailed() {
        // Given: Idempotency key exists with FAILED status
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .id(1L)
                .key("idempotency-123")
                .status(IdempotencyKeyStatus.FAILED)
                .build();

        // Simulate key already exists
        when(idempotencyService.tryCreateIdempotencyKey("idempotency-123")).thenReturn(false);
        when(idempotencyKeyRepository.findByKey("idempotency-123")).thenReturn(Optional.of(idempotencyKey));
        when(idempotencyService.handleExistingIdempotencyKey(any(), any()))
                .thenThrow(new IdempotencyConflictException("Onceki islem basarisiz oldu. Lutfen yeni bir idempotency key ile deneyin."));

        // When/Then
        IdempotencyConflictException exception = assertThrows(IdempotencyConflictException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"), "idempotency-123")
        );

        assertTrue(exception.getMessage().contains("basarisiz oldu"));
    }

    @Test
    void execute_differentIdempotencyKeys_allowConcurrentTransfers() {
        // Given: Two wallets with sufficient balance
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> {
            Transaction t = i.getArgument(0);
            if (t.getId() == null) {
                t = Transaction.builder()
                        .id(1L)
                        .fromWalletId(t.getFromWalletId())
                        .toWalletId(t.getToWalletId())
                        .amount(t.getAmount())
                        .idempotencyKey(t.getIdempotencyKey())
                        .build();
            }
            return t;
        });

        // Mock IdempotencyService methods
        when(idempotencyService.tryCreateIdempotencyKey("key-1")).thenReturn(true);
        when(idempotencyService.tryCreateIdempotencyKey("key-2")).thenReturn(true);
        doNothing().when(idempotencyService).completeIdempotencyKey(any(), any());

        // When: Execute two transfers with different idempotency keys
        Transaction result1 = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("10.00"), "key-1");
        Transaction result2 = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("20.00"), "key-2");

        // Then: Both transfers succeed with correct amounts
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(new BigDecimal("10.00"), result1.getAmount());
        assertEquals(new BigDecimal("20.00"), result2.getAmount());

        // Verify idempotency key creation was attempted for both
        verify(idempotencyService).tryCreateIdempotencyKey("key-1");
        verify(idempotencyService).tryCreateIdempotencyKey("key-2");
    }

    @Test
    void execute_transferWithoutIdempotencyKey_worksNormally() {
        // Given
        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(walletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // When: Execute without idempotency key
        Transaction result = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("30.00"));

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getFromWalletId());
        assertEquals(2L, result.getToWalletId());
        assertEquals(new BigDecimal("70.00"), fromWallet.getBalance());
        assertEquals(new BigDecimal("80.00"), toWallet.getBalance());

        // Verify no idempotency key operations
        verify(idempotencyKeyRepository, never()).save(any());
        verify(idempotencyKeyRepository, never()).findByKey(any());
    }

    @Test
    void execute_walletLocksAcquiredInDeterministicOrder_smallUserIdFirst() {
        // Given: fromUserId > toUserId (5 > 3), yani lock sırası 3 -> 5 olmalı
        Wallet fromWallet = Wallet.builder()
                .id(5L)
                .userId(5L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(3L)
                .userId(3L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        // Deterministik sıra: küçük ID (3) önce, sonra büyük ID (5)
        when(walletRepository.findByUserIdWithLock(3L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.findByUserIdWithLock(5L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // When: 5 -> 3 transfer (fromUserId > toUserId)
        Transaction result = transferMoneyUseCase.execute(5L, 3L, new BigDecimal("30.00"));

        // Then: İş kuralı korunmalı - fromUser (5) azalır, toUser (3) artar
        assertNotNull(result);
        assertEquals(5L, result.getFromWalletId());
        assertEquals(3L, result.getToWalletId());
        assertEquals(new BigDecimal("70.00"), fromWallet.getBalance());
        assertEquals(new BigDecimal("80.00"), toWallet.getBalance());

        // Verify: Lock'lar deterministik sırada alındı (önce 3, sonra 5)
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).findByUserIdWithLock(eq(3L));
        inOrder.verify(walletRepository).findByUserIdWithLock(eq(5L));
    }

    @Test
    void execute_walletLocksAcquiredInDeterministicOrder_sameOrderRegardlessOfFromTo() {
        // Given: fromUserId < toUserId (2 < 4), yani lock sırası 2 -> 4 olmalı
        Wallet fromWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(4L)
                .userId(4L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        // Deterministik sıra: küçük ID (2) önce, sonra büyük ID (4)
        when(walletRepository.findByUserIdWithLock(2L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserIdWithLock(4L)).thenReturn(Optional.of(toWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        // When: 2 -> 4 transfer (fromUserId < toUserId)
        Transaction result = transferMoneyUseCase.execute(2L, 4L, new BigDecimal("30.00"));

        // Then: İş kuralı korunmalı - fromUser (2) azalır, toUser (4) artar
        assertNotNull(result);
        assertEquals(2L, result.getFromWalletId());
        assertEquals(4L, result.getToWalletId());
        assertEquals(new BigDecimal("70.00"), fromWallet.getBalance());
        assertEquals(new BigDecimal("80.00"), toWallet.getBalance());

        // Verify: Lock'lar deterministik sırada alındı (önce 2, sonra 4)
        InOrder inOrder = inOrder(walletRepository);
        inOrder.verify(walletRepository).findByUserIdWithLock(eq(2L));
        inOrder.verify(walletRepository).findByUserIdWithLock(eq(4L));
    }

    @Test
    void execute_idempotencyKeyPayloadMismatch_throwsException() {
        // Given: Same idempotency key but different amount than original transaction
        IdempotencyKey idempotencyKey = IdempotencyKey.builder()
                .id(1L)
                .key("idempotency-123")
                .status(IdempotencyKeyStatus.COMPLETED)
                .transactionId(100L)
                .build();

        // Original transaction had amount 30.00, but new request has 50.00
        Transaction completedTransaction = Transaction.builder()
                .id(100L)
                .fromWalletId(1L)
                .toWalletId(2L)
                .amount(new BigDecimal("30.00"))
                .idempotencyKey("idempotency-123")
                .build();

        Wallet fromWallet = Wallet.builder()
                .id(1L)
                .userId(1L)
                .balance(new BigDecimal("100.00"))
                .currency("TRY")
                .build();

        Wallet toWallet = Wallet.builder()
                .id(2L)
                .userId(2L)
                .balance(new BigDecimal("50.00"))
                .currency("TRY")
                .build();

        when(idempotencyService.tryCreateIdempotencyKey("idempotency-123")).thenReturn(false);
        when(idempotencyKeyRepository.findByKey("idempotency-123")).thenReturn(Optional.of(idempotencyKey));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(fromWallet));
        when(walletRepository.findByUserId(2L)).thenReturn(Optional.of(toWallet));
        when(transactionRepository.findByIdempotencyKey("idempotency-123")).thenReturn(Optional.of(completedTransaction));
        when(idempotencyService.handleExistingIdempotencyKeyWithPayloadCheck(any(), any(), any(), any(), any()))
                .thenThrow(new IdempotencyPayloadMismatchException("Ayni idempotency key ile farkli parametreler gonderildi"));

        // When/Then: Payload mismatch should throw exception
        IdempotencyPayloadMismatchException exception = assertThrows(IdempotencyPayloadMismatchException.class, () ->
                transferMoneyUseCase.execute(1L, 2L, new BigDecimal("50.00"), "idempotency-123")
        );

        assertTrue(exception.getMessage().contains("farkli parametreler"));
    }
}
