package com.alper.digitalwallet.application.usecase;
import com.alper.digitalwallet.domain.exception.IdempotencyConflictException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferMoneyUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private TransferMoneyUseCase transferMoneyUseCase;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        transferMoneyUseCase = org.mockito.Mockito.spy(new TransferMoneyUseCase(
                walletRepository, transactionRepository, idempotencyKeyRepository));
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
        when(walletRepository.findByUserIdWithLock(999L)).thenReturn(Optional.empty());

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

        // Simulate key already exists (save throws exception)
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(idempotencyKeyRepository.findByKey("idempotency-123"))
                .thenReturn(Optional.of(idempotencyKey));
        when(transactionRepository.findByIdempotencyKey("idempotency-123"))
                .thenReturn(Optional.of(completedTransaction));

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

        // Simulate key already exists (save throws exception)
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(idempotencyKeyRepository.findByKey("idempotency-123"))
                .thenReturn(Optional.of(idempotencyKey));

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

        // Simulate key already exists (save throws exception)
        when(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(idempotencyKeyRepository.findByKey("idempotency-123"))
                .thenReturn(Optional.of(idempotencyKey));

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

        // Mock REQUIRES_NEW methods to work in unit test (no real transaction)
        org.mockito.Mockito.doReturn(true).when(transferMoneyUseCase).tryCreateIdempotencyKey("key-1");
        org.mockito.Mockito.doReturn(true).when(transferMoneyUseCase).tryCreateIdempotencyKey("key-2");
        org.mockito.Mockito.doNothing().when(transferMoneyUseCase).completeIdempotencyKey(any(), any());

        // When: Execute two transfers with different idempotency keys
        Transaction result1 = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("10.00"), "key-1");
        Transaction result2 = transferMoneyUseCase.execute(1L, 2L, new BigDecimal("20.00"), "key-2");

        // Then: Both transfers succeed with correct amounts
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(new BigDecimal("10.00"), result1.getAmount());
        assertEquals(new BigDecimal("20.00"), result2.getAmount());

        // Verify idempotency key creation was attempted for both
        verify(transferMoneyUseCase).tryCreateIdempotencyKey("key-1");
        verify(transferMoneyUseCase).tryCreateIdempotencyKey("key-2");
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
}
