package com.alper.digitalwallet.infrastructure.rest;

import com.alper.digitalwallet.application.usecase.CreateWalletUseCase;
import com.alper.digitalwallet.application.usecase.DepositMoneyUseCase;
import com.alper.digitalwallet.application.usecase.GetTransactionsUseCase;
import com.alper.digitalwallet.application.usecase.GetWalletUseCase;
import com.alper.digitalwallet.application.usecase.TransferMoneyUseCase;
import com.alper.digitalwallet.application.usecase.WithdrawMoneyUseCase;
import com.alper.digitalwallet.domain.model.Transaction;
import com.alper.digitalwallet.domain.model.Wallet;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WalletControllerMockMvcTest {

    private MockMvc mockMvc;

    @Mock
    private CreateWalletUseCase createWalletUseCase;

    @Mock
    private DepositMoneyUseCase depositMoneyUseCase;

    @Mock
    private WithdrawMoneyUseCase withdrawMoneyUseCase;

    @Mock
    private TransferMoneyUseCase transferMoneyUseCase;

    @Mock
    private GetWalletUseCase getWalletUseCase;

    @Mock
    private GetTransactionsUseCase getTransactionsUseCase;

    @BeforeEach
    void setUp() {
        WalletController walletController = new WalletController(
                createWalletUseCase,
                depositMoneyUseCase,
                withdrawMoneyUseCase,
                transferMoneyUseCase,
                getWalletUseCase,
                getTransactionsUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(walletController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createWalletReturnsForbiddenWhenBodyUserDiffersFromTokenUser() throws Exception {
        mockMvc.perform(post("/api/v1/wallets")
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"currency\":\"TRY\"}"))
                .andExpect(status().isForbidden());

        verify(createWalletUseCase, never()).execute(anyLong(), any());
    }

    @Test
    void createWalletReturnsCreatedWhenUserMatches() throws Exception {
        when(createWalletUseCase.execute(1L, "TRY")).thenReturn(Wallet.builder().id(1L).userId(1L).currency("TRY").build());

        mockMvc.perform(post("/api/v1/wallets")
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"currency\":\"TRY\"}"))
                .andExpect(status().isCreated());

        verify(createWalletUseCase).execute(1L, "TRY");
    }

    @Test
    void depositReturnsForbiddenWhenBodyUserDiffersFromTokenUser() throws Exception {
        mockMvc.perform(post("/api/v1/wallets/deposit")
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":2,\"amount\":10.00}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getWalletReturnsForbiddenWhenQueryUserDiffersFromTokenUser() throws Exception {
        mockMvc.perform(get("/api/v1/wallets")
                        .with(authenticatedUser(1L))
                        .param("userId", "2"))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferUsesAuthenticatedUserAsSender() throws Exception {
        when(transferMoneyUseCase.execute(eq(1L), eq(2L), any(), any())).thenReturn(Transaction.builder().id(1L).build());

        mockMvc.perform(post("/api/v1/wallets/transfer")
                        .with(authenticatedUser(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toUserId\":2,\"amount\":50.00}"))
                .andExpect(status().isCreated());

        verify(transferMoneyUseCase).execute(eq(1L), eq(2L), any(), any());
    }

    @Test
    void getWalletByIdReturnsForbiddenWhenTokenUserDiffersWalletOwner() throws Exception {
        when(getWalletUseCase.executeById(10L)).thenReturn(Wallet.builder().id(10L).userId(2L).build());

        mockMvc.perform(get("/api/v1/wallets/10")
                        .with(authenticatedUser(1L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTransactionsReturnsForbiddenWhenTokenUserDiffersWalletOwner() throws Exception {
        when(getWalletUseCase.executeById(10L)).thenReturn(Wallet.builder().id(10L).userId(2L).build());

        mockMvc.perform(get("/api/v1/wallets/10/transactions")
                        .with(authenticatedUser(1L)))
                .andExpect(status().isForbidden());

        verify(getTransactionsUseCase, never()).execute(eq(10L), any(Pageable.class));
    }

    @Test
    void getTransactionsAppliesAscSortFromPageable() throws Exception {
        Page<Transaction> page = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "amount")),
                0
        );
        when(getWalletUseCase.executeById(10L)).thenReturn(Wallet.builder().id(10L).userId(1L).build());
        when(getTransactionsUseCase.execute(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/wallets/10/transactions")
                        .with(authenticatedUser(1L))
                        .param("sortBy", "amount")
                        .param("direction", "ASC")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(getTransactionsUseCase).execute(eq(10L), captor.capture());
        Pageable pageable = captor.getValue();

        assertEquals("amount: ASC", pageable.getSort().toString());
    }

    @Test
    void getTransactionsAppliesDescSortFromPageable() throws Exception {
        Page<Transaction> page = new PageImpl<>(
                List.of(Transaction.builder().id(1L).build()),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "transactionDate")),
                1
        );
        when(getWalletUseCase.executeById(10L)).thenReturn(Wallet.builder().id(10L).userId(1L).build());
        when(getTransactionsUseCase.execute(eq(10L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/wallets/10/transactions")
                        .with(authenticatedUser(1L))
                        .param("sortBy", "transactionDate")
                        .param("direction", "DESC"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(getTransactionsUseCase).execute(eq(10L), captor.capture());
        Pageable pageable = captor.getValue();

        assertEquals("transactionDate: DESC", pageable.getSort().toString());
    }

    @Test
    void getTransactionsReturnsBadRequestForInvalidSortBy() throws Exception {
        when(getWalletUseCase.executeById(10L)).thenReturn(Wallet.builder().id(10L).userId(1L).build());

        mockMvc.perform(get("/api/v1/wallets/10/transactions")
                        .with(authenticatedUser(1L))
                        .param("sortBy", "nonExistingField")
                        .param("direction", "ASC"))
                .andExpect(status().isBadRequest());
    }

    private RequestPostProcessor authenticatedUser(Long userId) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList()));
            return request;
        };
    }
}

