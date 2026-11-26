package com.banking.account.service;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import com.banking.account.dto.BalanceOperationRequest;
import com.banking.account.dto.BalanceOperationResponse;
import jakarta.validation.Valid;

public interface AccountService {

    String createNewAccount(@Valid AccountCreationRequest request);

    AccountDetail fetchAccountDetail(String accountNo, int page, int size);

    BalanceOperationResponse reserveBalance(String accountNo, @Valid BalanceOperationRequest request);

    BalanceOperationResponse releaseReservation(String accountNo, @Valid BalanceOperationRequest request);

    BalanceOperationResponse debitBalance(String accountNo, @Valid BalanceOperationRequest request);

    BalanceOperationResponse creditBalance(String accountNo, @Valid BalanceOperationRequest request);
}
