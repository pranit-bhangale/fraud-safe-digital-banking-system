package com.banking.utility.version.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VersionParserTest {

    private final VersionParser parser = new VersionParser();

    @Test
    @DisplayName("Should successfully parse valid version string v1 to integer 1")
    void shouldParseVersionOne() {
        final Integer version = parser.parseVersion("v1");
        assertNotNull(version);
        assertEquals(1, version);
    }

    @Test
    @DisplayName("Should successfully parse multi-digit version string v12 to integer 12")
    void shouldParseMultiDigitVersion() {
        final Integer version = parser.parseVersion("v12");
        assertNotNull(version);
        assertEquals(12, version);
    }

    @ParameterizedTest
    @ValueSource(strings = {"v", "1", "V1", "version1", "v-1", "v1.0", "", "   "})
    @DisplayName("Should throw IllegalArgumentException for invalid version strings")
    void shouldThrowForInvalidVersion(final String invalidVersion) {
        assertThrows(IllegalArgumentException.class, () -> parser.parseVersion(invalidVersion));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when version is null")
    void shouldThrowWhenNull() {
        assertThrows(IllegalArgumentException.class, () -> parser.parseVersion(null));
    }
}
