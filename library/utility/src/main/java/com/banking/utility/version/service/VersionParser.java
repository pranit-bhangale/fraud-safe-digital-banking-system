package com.banking.utility.version.service;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.accept.ApiVersionParser;

import java.util.regex.Pattern;

/**
 * Parser for API version strings formatted as {@code v{number}} (e.g., "v1", "v2").
 */
@NullMarked
public final class VersionParser implements ApiVersionParser<Integer> {

    private static final Pattern VERSION_PATTERN = Pattern.compile("^v\\d+$");

    /**
     * Constructs a new {@link VersionParser} instance.
     */
    public VersionParser() {
    }

    /**
     * Parses the version string into an integer version number.
     *
     * @param version the raw version string (e.g., "v1")
     * @return the integer version number (e.g., 1)
     * @throws IllegalArgumentException if the version string is null, blank, or does not match the format {@code v{number}}
     */
    @Override
    public Integer parseVersion(final String version) {
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("Invalid API version: " + version);
        }
        return Integer.parseInt(version.substring(1));
    }
}
