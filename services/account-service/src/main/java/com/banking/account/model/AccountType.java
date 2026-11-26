package com.banking.account.model;

import java.math.BigDecimal;

public enum AccountType {
    SAVING(new BigDecimal("100.00"), new BigDecimal("25000.00")),
    CURRENT(new BigDecimal("500.00"), new BigDecimal("100000.00")),
    BUSINESS(new BigDecimal("1000.00"), new BigDecimal("500000.00")),
    SALARY(BigDecimal.ZERO, new BigDecimal("50000.00")),
    FIXED_DEPOSIT(new BigDecimal("1000.00"), BigDecimal.ZERO);

    private final BigDecimal minimumOpeningBalance;
    private final BigDecimal dailyTransferLimit;

    AccountType(BigDecimal minimumOpeningBalance, BigDecimal dailyTransferLimit) {
        this.minimumOpeningBalance = minimumOpeningBalance;
        this.dailyTransferLimit = dailyTransferLimit;
    }

    public BigDecimal getMinimumOpeningBalance() {
        return minimumOpeningBalance;
    }

    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }
}
