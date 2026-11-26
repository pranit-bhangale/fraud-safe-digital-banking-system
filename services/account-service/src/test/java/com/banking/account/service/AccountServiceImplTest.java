package com.banking.account.service;

import com.banking.account.client.TransactionGrpcClient;
import com.banking.account.client.UserFeignClient;
import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.entity.Account;
import com.banking.account.exception.AccountException;
import com.banking.account.model.AccountType;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.ProcessedTransactionRepository;
import com.banking.transaction.grpc.CreateInitialTransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository repository;

    @Mock
    private UserFeignClient userFeignClient;

    @Mock
    private TransactionGrpcClient transactionGrpcClient;

    @Mock
    private ProcessedTransactionRepository processedTransactionRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Edge Case 1: Rejects account creation when initial deposit is below minimum required balance")
    void shouldRejectWhenDepositBelowMinimum() {
        // SAVING requires min $100.00
        AccountCreationRequest request = AccountCreationRequest.builder()
                .userId(userId)
                .type(AccountType.SAVING)
                .balance(new BigDecimal("50.00")) // Below min $100
                .build();

        AccountException ex = assertThrows(AccountException.class, () -> accountService.createNewAccount(request));
        assertEquals("INSUFFICIENT_INITIAL_DEPOSIT", ex.getErrorCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Edge Case 2: Successfully creates account and activates after gRPC transaction confirms")
    void shouldCreateAccountSuccessfully() {
        AccountCreationRequest request = AccountCreationRequest.builder()
                .userId(userId)
                .type(AccountType.SAVING)
                .balance(new BigDecimal("250.00"))
                .build();

        when(userFeignClient.existsByUserId(userId)).thenReturn(CompletableFuture.completedFuture(true));
        when(repository.existsByAccountNo(any())).thenReturn(false);
        when(repository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionGrpcClient.createInitialTransaction(any(Account.class)))
                .thenReturn(CompletableFuture.completedFuture(
                        CreateInitialTransactionResponse.newBuilder().setSuccess(true).build()
                ));

        String accountNo = accountService.createNewAccount(request);

        assertNotNull(accountNo);
        verify(repository).save(any(Account.class));
        verify(transactionGrpcClient).createInitialTransaction(any(Account.class));
    }
}
