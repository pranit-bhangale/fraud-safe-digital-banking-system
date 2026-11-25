package com.banking.transaction.service;

import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    @Override
    public TransactionResponse createNewTransaction(TransactionRequest request) {
        // Check account no and validate amount through feign client
        // forward to fraud detection service
        
        return null;
    }
}
