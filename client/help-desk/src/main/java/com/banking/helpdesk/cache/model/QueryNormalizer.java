package com.banking.helpdesk.cache.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class QueryNormalizer {

    public static String normalize(String query) {
        if (query == null) return "";
        return query
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", " ");
    }
}
