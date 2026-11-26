package com.banking.account.client;

import com.banking.account.dto.AccountDetail;
import com.banking.account.entity.Account;
import com.banking.account.exception.GrpcException;
import com.banking.transaction.grpc.CreateInitialTransactionRequest;
import com.banking.transaction.grpc.CreateInitialTransactionResponse;
import com.banking.transaction.grpc.GetUserTransactionHistoryRequest;
import com.banking.transaction.grpc.TransactionInformation;
import com.banking.transaction.grpc.TransactionServiceGrpc;
import com.google.protobuf.Timestamp;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TransactionGrpcClient {

    private static final long TIMEOUT_SECONDS = 10;
    private static final int HISTORY_PAGE_SIZE = 100;

    private final TransactionServiceGrpc.TransactionServiceBlockingStub stub;

    public TransactionGrpcClient(GrpcChannelFactory factory) {
        this.stub = TransactionServiceGrpc.newBlockingStub(
                factory.createChannel("transaction-service"));
    }

    private static Timestamp toTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    public CreateInitialTransactionResponse createInitialTransaction(Account account) {
        CreateInitialTransactionRequest request = CreateInitialTransactionRequest.newBuilder()
                .setUserId(account.getUserId().toString())
                .setAccountId(account.getId().toString())
                .setAccountNo(account.getAccountNo())
                .setAccountType(account.getAccountType().name())
                .setBalance(account.getBalance().toPlainString())
                .setDescription("Initial account opening")
                .setTransactionDate(toTimestamp(Instant.now()))
                .build();
        try {
            return stubWithDeadline().createInitialTransaction(request);
        } catch (StatusRuntimeException e) {
            log.error("createInitialTransaction failed: {}", e.getStatus(), e);
            throw new GrpcException("Failed to create initial transaction" + e);
        }
    }

    public List<AccountDetail.AccountInformation.TransactionHistory> getTransactionHistory(
            UUID userId, String accountNo) {
        GetUserTransactionHistoryRequest request = GetUserTransactionHistoryRequest.newBuilder()
                .setUserId(userId.toString())
                .setAccountNo(accountNo)
                .setPage(0)
                .setSize(HISTORY_PAGE_SIZE)
                .build();
        try {
            return stubWithDeadline().getUserTransactionHistory(request)
                    .getTransactionsList().stream()
                    .map(this::toHistory)
                    .toList();
        } catch (StatusRuntimeException e) {
            log.error("getUserTransactionHistory failed: {}", e.getStatus(), e);
            throw new GrpcException("Failed to fetch transaction history" + e);
        }
    }

    private AccountDetail.AccountInformation.TransactionHistory toHistory(TransactionInformation t) {
        return AccountDetail.AccountInformation.TransactionHistory.builder()
                .transactionId(UUID.fromString(t.getTransactionId()))
                .referenceNo(t.getReferenceNo())
                .type(t.getType())
                .amount(new BigDecimal(t.getAmount()))
                .balanceAfter(new BigDecimal(t.getBalanceAfter()))
                .description(t.getDescription())
                .transactionDate(toInstant(t.getTransactionDate()))
                .build();
    }

    private TransactionServiceGrpc.TransactionServiceBlockingStub stubWithDeadline() {
        return stub.withDeadlineAfter(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}