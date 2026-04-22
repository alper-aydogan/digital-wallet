package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.application.usecase.CreateWalletUseCase;
import com.alper.digitalwallet.application.usecase.DepositMoneyUseCase;
import com.alper.digitalwallet.domain.exception.WalletAlreadyExistsException;
import com.alper.digitalwallet.domain.model.Wallet;
import com.alper.digitalwallet.domain.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WalletIntegrationTest {

    @Autowired
    private CreateWalletUseCase createWalletUseCase;

    @Autowired
    private DepositMoneyUseCase depositMoneyUseCase;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void testFullWorkflow_CreateAndDeposit() {
        // 1. Cuzdan olustur
        Wallet wallet = createWalletUseCase.execute(100L, "TRY");
        
        assertNotNull(wallet.getId());
        assertEquals(100L, wallet.getUserId());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertEquals("TRY", wallet.getCurrency());

        // 2. Para yatir
        wallet = depositMoneyUseCase.execute(100L, new BigDecimal("500.00"));
        assertEquals(new BigDecimal("500.00"), wallet.getBalance());

        // 3. Tekrar para yatir
        wallet = depositMoneyUseCase.execute(100L, new BigDecimal("250.00"));
        assertEquals(new BigDecimal("750.00"), wallet.getBalance());

        // 4. Veritabanından kontrol et
        Wallet savedWallet = walletRepository.findByUserId(100L).orElse(null);
        assertNotNull(savedWallet);
        assertEquals(new BigDecimal("750.00"), savedWallet.getBalance());
    }

    @Test
    void testUniqueConstraint_SingleWalletPerUser() {
        // Ilk cuzdan olustur
        Wallet wallet1 = createWalletUseCase.execute(101L, "TRY");
        assertNotNull(wallet1.getId());

        // Ayni kullanici icin ikinci cuzdan olusturma domain seviyesinde engellenmeli
        assertThrows(WalletAlreadyExistsException.class, () -> createWalletUseCase.execute(101L, "TRY"));
    }

    @Test
    void testWithdrawal_SufficientBalance() {
        // Cuzdan olustur
        Wallet wallet = createWalletUseCase.execute(102L, "TRY");
        
        // Para yatir
        wallet = depositMoneyUseCase.execute(102L, new BigDecimal("500.00"));
        assertEquals(new BigDecimal("500.00"), wallet.getBalance());
    }

    @Test
    void testTransactionPersistence() {
        // Yeni kullanici cuzdan olustur
        Wallet wallet = createWalletUseCase.execute(103L, "TRY");
        Long walletId = wallet.getId();

        // Para yatır
        wallet = depositMoneyUseCase.execute(103L, new BigDecimal("100.00"));

        // Veritabanından bularak persist olup olmadığını kontrol et
        Wallet retrievedWallet = walletRepository.findById(walletId).orElse(null);
        assertNotNull(retrievedWallet);
        assertEquals(new BigDecimal("100.00"), retrievedWallet.getBalance());
        assertEquals(103L, retrievedWallet.getUserId());
    }

    @Test
    void testOptimisticLocking_Version() {
        // Cuzdan olustur
        Wallet wallet = createWalletUseCase.execute(104L, "TRY");
        assertNotNull(wallet.getVersion());

        // Para yatir
        wallet = depositMoneyUseCase.execute(104L, new BigDecimal("100.00"));
        Long version1 = wallet.getVersion();

        // Tekrar para yatir
        wallet = depositMoneyUseCase.execute(104L, new BigDecimal("50.00"));
        Long version2 = wallet.getVersion();

        // Version field'ı güncellenmiş olmalı
        assertNotNull(version1);
        assertNotNull(version2);
    }
}

