package com.banking.transaction.service;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.grpc.CreateInitialTransactionRequest;
import com.banking.transaction.grpc.CreateInitialTransactionResponse;
import com.banking.transaction.grpc.GetUserTransactionHistoryRequest;
import com.banking.transaction.grpc.GetUserTransactionHistoryResponse;
import com.banking.transaction.grpc.TransactionInformation;
import com.banking.transaction.grpc.TransactionServiceGrpc;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.repository.TransactionRepository;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    public static final String TYPE_OPENING = "OPENING";
    private static final int MAX_PAGE_SIZE = 200;
    private final TransactionRepository repository;

    private static TransactionInformation toProto(Transaction t) {
        return TransactionInformation.newBuilder()
                .setTransactionId(t.getId().toString())
                .setReferenceNo(t.getReferenceNo())
                .setType(String.valueOf(t.getType()))
                .setAmount(t.getAmount().toPlainString())
                .setBalanceAfter(t.getBalanceAfter().toPlainString())
                .setDescription(t.getDescription() == null ? "" : t.getDescription())
                .setTransactionDate(toTimestamp(t.getTransactionDate()))
                .build();
    }

    private static Timestamp toTimestamp(Instant i) {
        return Timestamp.newBuilder().setSeconds(i.getEpochSecond()).setNanos(i.getNano()).build();
    }

    private static Instant toInstant(Timestamp ts) {
        return Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    }

    @Override
    public void createInitialTransaction(
            CreateInitialTransactionRequest request,
            StreamObserver<CreateInitialTransactionResponse> observer
    ) {
        try {
            if (!request.hasTransactionDate()) {
                throw new IllegalArgumentException("transaction_date is required");
            }
            if (request.getAccountNo().isBlank()) {
                throw new IllegalArgumentException("account_no is required");
            }
            BigDecimal balance = new BigDecimal(request.getBalance());
            if (balance.signum() < 0) {
                throw new IllegalArgumentException("balance must not be negative");
            }
            Transaction txn = createInitialTransaction(
                    UUID.fromString(request.getUserId()), UUID.fromString(request.getAccountId()),
                    request.getAccountNo(), request.getAccountType(),
                    balance, request.getDescription(),
                    toInstant(request.getTransactionDate())
            );
            observer.onNext(CreateInitialTransactionResponse.newBuilder()
                    .setSuccess(true)
                    .setTransactionId(txn.getId().toString())
                    .setMessage("Initial transaction created")
                    .build());
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("createInitialTransaction invalid request: {}", e.getMessage());
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("createInitialTransaction failed", e);
            observer.onError(Status.INTERNAL.withDescription("Failed to create initial transaction").asRuntimeException());
        }
    }

    @Override
    public void getUserTransactionHistory(
            GetUserTransactionHistoryRequest request,
            StreamObserver<GetUserTransactionHistoryResponse> observer) {
        try {
            if (request.getAccountNo().isBlank()) {
                throw new IllegalArgumentException("account_no is required");
            }
            Page<Transaction> page = getHistory(
                    UUID.fromString(request.getUserId()), request.getAccountNo(),
                    request.getPage(), request.getSize()
            );
            GetUserTransactionHistoryResponse.Builder response = GetUserTransactionHistoryResponse.newBuilder()
                    .setPage(page.getNumber())
                    .setSize(page.getSize())
                    .setTotalElements(page.getTotalElements())
                    .setTotalPages(page.getTotalPages())
                    .setHasNext(page.hasNext())
                    .setHasPrevious(page.hasPrevious());
            page.getContent().forEach(t -> response.addTransactions(toProto(t)));
            observer.onNext(response.build());
            observer.onCompleted();
        } catch (IllegalArgumentException e) {
            log.warn("getUserTransactionHistory invalid request: {}", e.getMessage());
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("getUserTransactionHistory failed", e);
            observer.onError(Status.INTERNAL.withDescription("Failed to fetch transaction history").asRuntimeException());
        }
    }

    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Transaction createInitialTransaction(
            UUID userId, UUID accountId, String accountNo,
            String accountType, BigDecimal balance,
            String description, Instant date
    ) {
        return repository.findFirstByAccountIdAndType(accountId, TYPE_OPENING)
                .orElseGet(() -> repository.save(Transaction.builder()
                        .userId(userId)
                        .accountNo(accountNo)
                        .accountType(accountType)
                        .referenceNo(generateReferenceNo())
                        .type(TransactionType.valueOf(TYPE_OPENING))
                        .amount(balance)
                        .balanceAfter(balance)
                        .description(description)
                        .transactionDate(date)
                        .build()));
    }

    @Transactional(readOnly = true)
    public Page<Transaction> getHistory(UUID userId, String accountNo, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return repository.findByUserIdAndAccountNoOrderByTransactionDateDesc
                (userId, accountNo, PageRequest.of(safePage, safeSize));
    }

    private String generateReferenceNo() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}