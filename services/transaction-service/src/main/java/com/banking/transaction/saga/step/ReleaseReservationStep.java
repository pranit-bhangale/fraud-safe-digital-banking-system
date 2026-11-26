package com.banking.transaction.saga.step;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.BalanceOperationRequest;
import com.banking.transaction.dto.BalanceOperationResponse;
import com.banking.transaction.saga.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Compensation step: Release sender balance reservation.
 * Called when fraud rejects or saga fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReleaseReservationStep {

    private final AccountClient accountClient;

    public void execute(SagaContext context) {
        log.info("Releasing reservation: txnId={}, sagaId={}, account={}, amount={}",
                context.referenceNo(), context.sagaId(), context.senderAccountNo(), context.amount());

        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .transactionId(context.transactionId())
                .sagaId(context.sagaId())
                .amount(context.amount())
                .description("Release reservation for failed " + context.transactionType().name())
                .build();

        BalanceOperationResponse response = accountClient.releaseReservation(context.senderAccountNo(), request);

        if (!response.success()) {
            log.error("Failed to release reservation: txnId={}, sagaId={}, error={}",
                    context.referenceNo(), context.sagaId(), response.message());
            // Don't throw - we want to mark saga as failed and alert for manual intervention
            throw new ReleaseReservationException("Failed to release reservation: " + response.message());
        }

        log.info("Reservation released successfully: txnId={}, sagaId={}, availableBalance={}",
                context.referenceNo(), context.sagaId(), response.availableBalance());
    }

    public static class ReleaseReservationException extends RuntimeException {
        public ReleaseReservationException(String message) {
            super(message);
        }
    }
}