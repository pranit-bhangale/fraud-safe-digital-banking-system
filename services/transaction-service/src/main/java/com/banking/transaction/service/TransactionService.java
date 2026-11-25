package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;

import java.util.UUID;

public interface TransactionService {

    TransactionResponse createNewTransaction(TransactionRequest request);

    TransactionResponse getTransactionByReference(String referenceNo);

    TransactionResponse getTransactionById(UUID id);
}
