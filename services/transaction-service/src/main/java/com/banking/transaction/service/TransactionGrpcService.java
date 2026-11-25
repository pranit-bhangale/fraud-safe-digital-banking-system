package com.banking.transaction.service;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.grpc.TransactionHistory;
import com.banking.transaction.grpc.TransactionHistoryRequest;
import com.banking.transaction.grpc.TransactionHistoryResponse;
import com.banking.transaction.grpc.TransactionServiceGrpc;
import com.banking.transaction.repository.TransactionRepository;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class TransactionGrpcService extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionRepository repository;

    @Override
    public void getUserTransactionHistory(
            TransactionHistoryRequest request,
            StreamObserver<TransactionHistoryResponse> responseObserver
    ) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            int page = Math.max(request.getPage(), 0);
            int size = request.getSize() <= 0 ? 20 : Math.min(request.getSize(), 100);
            Pageable pageable = PageRequest.of(
                    page, size, Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("id"))
            );
            Page<Transaction> transactionPage = this.repository.findAllByUserIdOrderByTransactionDateDesc(userId, pageable);
            TransactionHistoryResponse.Builder response = TransactionHistoryResponse.newBuilder()
                    .setPage(transactionPage.getNumber())
                    .setSize(transactionPage.getSize())
                    .setTotalElements(transactionPage.getTotalElements())
                    .setTotalPages(transactionPage.getTotalPages())
                    .setHasNext(transactionPage.hasNext())
                    .setHasPrevious(transactionPage.hasPrevious());
            transactionPage.getContent().forEach(transaction -> {
                TransactionHistory history = TransactionHistory.newBuilder()
                        .setTransactionId(transaction.getId().toString())
                        .setUserId(transaction.getUserId().toString())
                        .setAccountId(transaction.getAccountId().toString())
                        .setReferenceNo(transaction.getReferenceNo())
                        .setType(transaction.getType().name())
                        .setAmount(transaction.getAmount().toPlainString())
                        .setBalanceAfter(transaction.getBalanceAfter().toPlainString())
                        .setDescription(transaction.getDescription() != null ? transaction.getDescription() : "")
                        .setTransactionDate(Timestamp.newBuilder()
                                .setSeconds(transaction.getTransactionDate().getEpochSecond())
                                .setNanos(transaction.getTransactionDate().getNano())
                                .build())
                        .build();
                response.addTransactions(history);
            });
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL.withDescription("Failed to fetch transaction history").asRuntimeException());
        }
    }
}
