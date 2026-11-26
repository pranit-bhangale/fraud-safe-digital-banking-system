package com.banking.account.service;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository repository;

    public static String generate() {
        return String.format
                ("%010d", RANDOM.nextLong(1_000_000_000L, 10_000_000_000L));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createNewAccount(AccountCreationRequest request) {
        // Check User exists through feign Client
        // if exists Create Account for User
        // Through gRPC update initial Transaction history
    }

    @Override
    public AccountDetail fetchAccountDetail(String accountNo) {
        // Fetch Account Detail
        // Fetch User Detail through feign Client
        // Fetch Transaction thorugh gRPC Unary Mode
        return null;
    }
}
