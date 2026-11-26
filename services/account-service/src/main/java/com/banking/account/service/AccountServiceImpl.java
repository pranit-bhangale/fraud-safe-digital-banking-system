package com.banking.account.service;

import com.banking.account.client.UserFeignClient;
import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.dto.BalanceOperationRequest;
import com.banking.account.dto.BalanceOperationResponse;
import com.banking.account.entity.Account;
import com.banking.account.entity.ProcessedTransaction;
import com.banking.account.exception.AccountException;
import com.banking.account.exception.ResourceNotFoundException;
import com.banking.account.model.AccountStatus;
import com.banking.account.model.BalanceOperationType;
import com.banking.account.repository.AccountRepository;
import com.banking.account.repository.ProcessedTransactionRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccountRepository repository;
    private final UserFeignClient userFeignClient;
    // private final TransactionGrpcClient transactionGrpcClient;
    private final ProcessedTransactionRepository processedTransactionRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public String createNewAccount(AccountCreationRequest request) {
        log.info("Processing account creation request: userId={}, type={}, requestedBalance={}",
                request.userId(), request.type(), request.balance());

        // Edge case 1: Validate minimum opening balance against account type policies
        BigDecimal minOpeningBalance = request.type().getMinimumOpeningBalance();
        if (request.balance() != null && request.balance().compareTo(minOpeningBalance) < 0) {
            log.warn("Account creation rejected: deposit {} is below minimum {} for type {}",
                    request.balance(), minOpeningBalance, request.type());
            throw new AccountException("Initial deposit of $" + request.balance() +
                    " is below the minimum required opening balance of $" + minOpeningBalance + " for " + request.type(),
                    HttpStatus.BAD_REQUEST, "INSUFFICIENT_INITIAL_DEPOSIT");
        }

        // Edge case 2: Verify user existence in user-service
        boolean userExists;
        try {
            userExists = this.userFeignClient.existsByUserId(request.userId()).join();
        } catch (CompletionException ex) {
            log.error("Failed to verify user existence: userId={}, error={}", request.userId(), ex.getCause().getMessage());
            throw new ResourceNotFoundException("User service unavailable: " + request.userId());
        }
        if (!userExists) {
            throw new ResourceNotFoundException("User not found: " + request.userId());
        }

        // Edge case 3: Collision-safe unique account number generation with retry loop
        String accountNo = generateUniqueAccountNumber();

        // Edge case 4: Account initially created in PENDING status until initial opening ledger transaction is recorded
        Account account = Account.builder()
                .accountNo(accountNo)
                .userId(request.userId())
                .accountType(request.type())
                .accountStatus(AccountStatus.PENDING)
                .balance(request.balance() != null ? request.balance() : BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .build();
        account = this.repository.save(account);

        // Edge case 5: Record opening ledger transaction via gRPC in transaction-service
//        CreateInitialTransactionResponse response;
//        try {
//            response = this.transactionGrpcClient.createInitialTransaction(account).join();
//        } catch (CompletionException ex) {
//            log.error("Failed to record initial transaction via gRPC: accountNo={}, error={}", account.getAccountNo(), ex.getCause().getMessage());
//            throw new GrpcException("Transaction service unavailable for opening deposit registration");
//        }
//        if (!response.getSuccess()) {
//            throw new GrpcException("Failed to record initial transaction: " + response.getMessage());
//        }

        // Activate account upon confirmed opening transaction
        account.setAccountStatus(AccountStatus.ACTIVE);
        this.repository.save(account);

        log.info("Account successfully created and activated: accountNo={}, userId={}, initialBalance={}",
                account.getAccountNo(), account.getUserId(), account.getBalance());
        return account.getAccountNo();
    }

    private String generateUniqueAccountNumber() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            String candidate = generateAccountNumber();
            if (!this.repository.existsByAccountNo(candidate)) {
                return candidate;
            }
            log.warn("Account number collision detected on attempt {}: candidate={}, retrying...", attempt, candidate);
        }
        throw new AccountException("Failed to generate a unique account number after multiple attempts",
                HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT_GENERATION_FAILED");
    }

    public static String generateAccountNumber() {
        int length = RANDOM.nextInt(12, 17);
        long min = (long) Math.pow(10, length - 1);
        long max = (long) Math.pow(10, length);
        return String.valueOf(RANDOM.nextLong(min, max));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, readOnly = true)
    public AccountDetail fetchAccountDetail(String accountNo, int page, int size) {
        Account account = this.repository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));
        AccountDetail.UserInformation user;
        try {
            user = this.userFeignClient.getUserById(account.getUserId()).join();
        } catch (CompletionException ex) {
            log.warn("User service unavailable, proceeding without user info: userId={}, error={}",
                    account.getUserId(), ex.getCause().getMessage());
            user = null;
        }
        AccountDetail.PaginatedTransactionHistory history;
        try {
//            history = this.transactionGrpcClient
//                    .getTransactionHistory(account.getUserId(), account.getAccountNo(), page, size)
//                    .join();
        } catch (CompletionException ex) {
            log.warn("Transaction service unavailable, returning empty history: accountNo={}, error={}",
                    accountNo, ex.getCause().getMessage());
            history = AccountDetail.PaginatedTransactionHistory.empty(page, size);
        }
        return AccountDetail.builder()
                .user(user)
                .account(AccountDetail.AccountInformation.builder()
                        .id(account.getId())
                        .accountNo(account.getAccountNo())
                        .type(account.getAccountType())
                        .status(account.getAccountStatus())
                        .balance(account.getBalance())
                        //.history(history)
                        .build())
                .build();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Retryable(retryFor = {OptimisticLockingFailureException.class, OptimisticLockException.class}, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public BalanceOperationResponse reserveBalance(String accountNo, BalanceOperationRequest request) {
        return executeBalanceOperation(accountNo, request, BalanceOperationType.RESERVE);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Retryable(retryFor = {OptimisticLockingFailureException.class, OptimisticLockException.class}, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public BalanceOperationResponse releaseReservation(String accountNo, BalanceOperationRequest request) {
        return executeBalanceOperation(accountNo, request, BalanceOperationType.RELEASE);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Retryable(retryFor = {OptimisticLockingFailureException.class, OptimisticLockException.class}, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public BalanceOperationResponse debitBalance(String accountNo, BalanceOperationRequest request) {
        return executeBalanceOperation(accountNo, request, BalanceOperationType.DEBIT);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Retryable(retryFor = {OptimisticLockingFailureException.class, OptimisticLockException.class}, maxAttempts = 3, backoff = @Backoff(delay = 50))
    public BalanceOperationResponse creditBalance(String accountNo, BalanceOperationRequest request) {
        return executeBalanceOperation(accountNo, request, BalanceOperationType.CREDIT);
    }

    private BalanceOperationResponse executeBalanceOperation(String accountNo, BalanceOperationRequest request, BalanceOperationType type) {
        String operationType = type.name();
        if (processedTransactionRepository.existsByTransactionIdAndSagaIdAndOperationType(
                request.transactionId(), request.sagaId(), type)) {
            var existing = processedTransactionRepository
                    .findByTransactionIdAndSagaIdAndOperationType(request.transactionId(), request.sagaId(), type)
                    .orElseThrow();
            log.info("Idempotent replay for {}: txnId={}, sagaId={}, returning cached result: {}",
                    operationType, request.transactionId(), request.sagaId(), existing.getResultStatus());
            return existing.getResultStatus().equals("SUCCESS")
                    ? BalanceOperationResponse.success(
                    request.transactionId(), request.sagaId(), accountNo,
                    existing.getAmount(), existing.getAmount(), BigDecimal.ZERO, existing.getResultMessage())
                    : BalanceOperationResponse.failure(
                    request.transactionId(), request.sagaId(), accountNo,
                    BigDecimal.ZERO, BigDecimal.ZERO, existing.getResultMessage());
        }

        Account account = this.repository.findByAccountNo(accountNo)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNo));

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            return recordAndReturnFailure(request, accountNo, type,
                    account.getBalance(), account.getReservedBalance(),
                    "Account not active: " + account.getAccountStatus());
        }

        BigDecimal previousBalance = account.getBalance();
        BigDecimal previousReserved = account.getReservedBalance();
        String message;
        try {
            switch (type) {
                case RESERVE -> {
                    if (!account.canReserve(request.amount())) {
                        throw new IllegalStateException("Insufficient available balance");
                    }
                    account.setReservedBalance(account.getReservedBalance().add(request.amount()));
                    message = "Balance reserved for transaction";
                }
                case RELEASE -> {
                    if (account.getReservedBalance().compareTo(request.amount()) < 0) {
                        throw new IllegalStateException("Reservation amount exceeds reserved balance");
                    }
                    account.setReservedBalance(account.getReservedBalance().subtract(request.amount()));
                    message = "Balance reservation released";
                }
                case DEBIT -> {
                    if (account.getReservedBalance().compareTo(request.amount()) >= 0) {
                        account.setBalance(account.getBalance().subtract(request.amount()));
                        account.setReservedBalance(account.getReservedBalance().subtract(request.amount()));
                        message = "Balance debited from reserved amount";
                    } else if (account.getAvailableBalance().compareTo(request.amount()) >= 0) {
                        account.setBalance(account.getBalance().subtract(request.amount()));
                        message = "Balance debited directly";
                    } else {
                        throw new IllegalStateException("Insufficient available balance for debit");
                    }
                }
                case CREDIT -> {
                    account.setBalance(account.getBalance().add(request.amount()));
                    message = "Balance credited";
                }
                default -> throw new IllegalArgumentException("Unknown operation type: " + type);
            }

            Account saved = this.repository.save(account);
            log.info("Balance operation {} for account {}: txnId={}, sagaId={}, amount={}, newBalance={}, reserved={}",
                    type, accountNo, request.transactionId(), request.sagaId(), request.amount(),
                    saved.getBalance(), saved.getReservedBalance());

            recordProcessedTransaction(request, accountNo, type, "SUCCESS", message, saved.getBalance(), saved.getReservedBalance());

            return BalanceOperationResponse.success(
                    request.transactionId(), request.sagaId(), accountNo,
                    previousBalance, saved.getBalance(), saved.getReservedBalance(), message);

        } catch (Exception e) {
            log.warn("Balance operation {} failed: txnId={}, sagaId={}, error={}",
                    type, request.transactionId(), request.sagaId(), e.getMessage());
            return recordAndReturnFailure(request, accountNo, type,
                    previousBalance, previousReserved, e.getMessage());
        }
    }

    private BalanceOperationResponse recordAndReturnFailure(BalanceOperationRequest request, String accountNo,
                                                            BalanceOperationType operationType, BigDecimal balance,
                                                            BigDecimal reservedBalance, String message) {
        recordProcessedTransaction(request, accountNo, operationType, "FAILURE", message, balance, reservedBalance);
        return BalanceOperationResponse.failure(
                request.transactionId(), request.sagaId(), accountNo,
                balance, reservedBalance, message);
    }

    private void recordProcessedTransaction(BalanceOperationRequest request, String accountNo,
                                            BalanceOperationType operationType, String resultStatus, String message,
                                            BigDecimal balance, BigDecimal reservedBalance) {
        ProcessedTransaction processed = ProcessedTransaction.builder()
                .id(java.util.UUID.randomUUID())
                .transactionId(request.transactionId())
                .sagaId(request.sagaId())
                .accountNo(accountNo)
                .operationType(operationType)
                .amount(request.amount())
                .resultStatus(resultStatus)
                .resultMessage(message)
                .build();
        processedTransactionRepository.save(processed);
    }
}