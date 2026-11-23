package com.banking.utility.service;

import lombok.experimental.UtilityClass;

import java.security.SecureRandom;

@UtilityClass
public class Generate {

    private static final String ALPHANUMERIC =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String eventId() {
        final StringBuilder id = new StringBuilder("event");
        for (int i = 0; i < 16; i++) {
            id.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return id.toString();
    }
}
