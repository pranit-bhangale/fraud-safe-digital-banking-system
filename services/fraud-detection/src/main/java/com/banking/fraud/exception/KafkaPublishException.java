package com.banking.fraud.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when event dispatching to the Kafka broker encounters an unrecoverable failure.
 */
public class KafkaPublishException extends FraudException {

    public KafkaPublishException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE, "KAFKA_PUBLISH_ERROR");
    }
}
