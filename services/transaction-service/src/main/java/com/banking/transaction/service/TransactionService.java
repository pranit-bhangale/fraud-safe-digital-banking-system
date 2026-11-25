package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;

public interface TransactionService {

    TransactionResponse createNewTransaction(TransactionRequest request);
}
