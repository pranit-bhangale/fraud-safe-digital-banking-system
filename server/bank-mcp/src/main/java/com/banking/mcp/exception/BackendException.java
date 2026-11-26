package com.banking.mcp.exception;

public class BackendException extends RuntimeException {
    public BackendException(String backend, int status) {
        super("Backend '" + backend + "' returned HTTP " + status);
    }
}