package com.banking.fraud.util;

import lombok.experimental.UtilityClass;

/**
 * Utility for masking sensitive account numbers and identifiers.
 */
@UtilityClass
public class MaskingUtil {

    private static final String MASK = "****";

    /**
     * Masks user identifiers or keys if present.
     */
    public static String maskUserId(String id) {
        return maskAccountNo(id);
    }

    /**
     * Masks account numbers: retains first 3 and last 4 characters.
     */
    public static String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) return "unknown";
        int len = accountNo.length();
        if (len <= 6) return MASK;
        return accountNo.substring(0, 3) + MASK + accountNo.substring(len - 4);
    }
}