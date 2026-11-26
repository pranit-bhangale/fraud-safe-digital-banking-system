package com.banking.transaction.saga;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.BalanceOperationRequest;
import com.banking.transaction.dto.BalanceOperationResponse;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.saga.step.UpdateBalanceStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateBalanceStepTest {

    @Mock
    private AccountClient accountClient;

    @InjectMocks
    private UpdateBalanceStep updateBalanceStep;

    @Test
    @DisplayName("Forward Flow: Debits sender and credits receiver on fraud approval")
    void shouldDebitSenderAndCreditReceiver() {
        SagaContext context = SagaContext.builder()
                .transactionId(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .referenceNo("TXN-12345")
                .senderAccountNo("ACC-1111")
                .receiverAccountNo("ACC-2222")
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.TRANSFER)
                .build();

        Event.PaymentApproved approved = Event.PaymentApproved.builder()
                .txnId("TXN-12345")
                .riskScore(10)
                .build();

        when(accountClient.debitBalance(eq("ACC-1111"), any(BalanceOperationRequest.class)))
                .thenReturn(BalanceOperationResponse.success(
                        context.transactionId(), context.sagaId(), "ACC-1111",
                        new BigDecimal("1000.00"), new BigDecimal("500.00"), BigDecimal.ZERO, "Debit OK"
                ));

        when(accountClient.creditBalance(eq("ACC-2222"), any(BalanceOperationRequest.class)))
                .thenReturn(BalanceOperationResponse.success(
                        context.transactionId(), context.sagaId(), "ACC-2222",
                        BigDecimal.ZERO, new BigDecimal("500.00"), BigDecimal.ZERO, "Credit OK"
                ));

        updateBalanceStep.execute(context, approved);

        verify(accountClient).debitBalance(eq("ACC-1111"), any(BalanceOperationRequest.class));
        verify(accountClient).creditBalance(eq("ACC-2222"), any(BalanceOperationRequest.class));
    }

    @Test
    @DisplayName("Compensation Flow: Automatically refunds sender if receiver credit fails")
    void shouldCompensateSenderWhenReceiverFails() {
        SagaContext context = SagaContext.builder()
                .transactionId(UUID.randomUUID())
                .sagaId(UUID.randomUUID())
                .referenceNo("TXN-12345")
                .senderAccountNo("ACC-1111")
                .receiverAccountNo("ACC-2222")
                .amount(new BigDecimal("500.00"))
                .transactionType(TransactionType.TRANSFER)
                .build();

        Event.PaymentApproved approved = Event.PaymentApproved.builder()
                .txnId("TXN-12345")
                .riskScore(10)
                .build();

        when(accountClient.debitBalance(eq("ACC-1111"), any(BalanceOperationRequest.class)))
                .thenReturn(BalanceOperationResponse.success(
                        context.transactionId(), context.sagaId(), "ACC-1111",
                        new BigDecimal("1000.00"), new BigDecimal("500.00"), BigDecimal.ZERO, "Debit OK"
                ));

        // Receiver credit fails
        when(accountClient.creditBalance(eq("ACC-2222"), any(BalanceOperationRequest.class)))
                .thenReturn(BalanceOperationResponse.failure(
                        context.transactionId(), context.sagaId(), "ACC-2222",
                        BigDecimal.ZERO, BigDecimal.ZERO, "Account frozen"
                ));

        // Compensating refund succeeds
        when(accountClient.creditBalance(eq("ACC-1111"), any(BalanceOperationRequest.class)))
                .thenReturn(BalanceOperationResponse.success(
                        context.transactionId(), context.sagaId(), "ACC-1111",
                        new BigDecimal("500.00"), new BigDecimal("1000.00"), BigDecimal.ZERO, "Refund OK"
                ));

        assertThrows(UpdateBalanceStep.UpdateBalanceException.class,
                () -> updateBalanceStep.execute(context, approved));

        // Verify compensation refund was dispatched to sender
        verify(accountClient).creditBalance(eq("ACC-1111"), any(BalanceOperationRequest.class));
    }
}
