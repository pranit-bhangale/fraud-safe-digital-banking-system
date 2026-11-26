package com.banking.account.service;

import com.banking.account.dto.AccountCreationRequest;
import com.banking.account.dto.AccountDetail;
import jakarta.validation.Valid;

public interface AccountService {

    void createNewAccount(@Valid AccountCreationRequest request);

    AccountDetail fetchAccountDetail(String accountNo);
}
