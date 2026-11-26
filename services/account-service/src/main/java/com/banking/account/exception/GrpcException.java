package com.banking.account.exception;

import org.springframework.http.HttpStatus;

public class GrpcException extends AccountException {
    public GrpcException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, "gRPC_RESOURCE_ERROR");
    }
}
