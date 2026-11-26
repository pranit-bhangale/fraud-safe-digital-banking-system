package com.banking.account.service;

import com.banking.account.client.TransactionGrpcClient;
import com.banking.account.client.UserFeignClient;
import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.entity.Account;
import com.banking.account.exception.GrpcException;
import com.banking.account.exception.ResourceNotFoundException;
import com.banking.account.model.AccountStatus;
import com.banking.account.repository.AccountRepository;
import com.banking.transaction.grpc.CreateInitialTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository repository;
    private final UserFeignClient userFeignClient;
    private final TransactionGrpcClient transactionGrpcClient;

    public static String generateAccountNumber() {
        int length = RANDOM.nextInt(12, 17);
        long min = (long) Math.pow(10, length - 1);
        long max = (long) Math.pow(10, length);
        return String.valueOf(RANDOM.nextLong(min, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void createNewAccount(AccountCreationRequest request) {
        boolean userExists = this.userFeignClient.existsByUserId(request.userId());
        if (!userExists) {
            throw new ResourceNotFoundException("User not found: " + request.userId());
        }
        Account account = Account.builder()
                .accountNo(generateAccountNumber())
                .userId(request.userId())
                .accountType(request.type())
                .accountStatus(AccountStatus.ACTIVE)
                .balance(request.balance())
                .build();
        account = this.repository.save(account);
        CreateInitialTransactionResponse response = this.transactionGrpcClient.createInitialTransaction(account);
        if (!response.getSuccess()) {
            throw new GrpcException("Failed to create initial transaction");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, readOnly = true)
    public AccountDetail fetchAccountDetail(String accountNo) {
        Account account = this.repository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));
        AccountDetail.UserInformation user = this.userFeignClient.getUserById(account.getUserId());
        List<AccountDetail.AccountInformation.TransactionHistory> history = this.transactionGrpcClient
                .getTransactionHistory(account.getUserId(), account.getAccountNo());
        return AccountDetail.builder()
                .user(user)
                .account(AccountDetail.AccountInformation.builder()
                        .id(account.getId())
                        .accountNo(account.getAccountNo())
                        .type(account.getAccountType())
                        .status(account.getAccountStatus())
                        .balance(account.getBalance())
                        .history(history)
                        .build())
                .build();
    }
}