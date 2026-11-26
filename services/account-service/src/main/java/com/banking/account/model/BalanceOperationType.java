package com.banking.account.model;

/**
 * Types of balance operations for saga pattern.
 */
public enum BalanceOperationType {
    RESERVE,
    RELEASE,
    DEBIT,
    CREDIT
}