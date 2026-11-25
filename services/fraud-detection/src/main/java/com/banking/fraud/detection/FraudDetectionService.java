package com.banking.fraud.detection;

import com.banking.fraud.constant.FraudConstant;
import com.banking.fraud.dto.DetectionResult;
import com.banking.fraud.dto.RiskResult;
import com.banking.fraud.event.Event;
import com.banking.fraud.exception.FraudException;
import com.banking.fraud.exception.InvalidPaymentException;
import com.banking.fraud.model.FraudStatus;
import com.banking.fraud.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.banking.fraud.constant.FraudConstant.ACTION_APPROVE;
import static com.banking.fraud.constant.FraudConstant.ACTION_REJECT_BLOCK;
import static com.banking.fraud.constant.FraudConstant.REASON_CHECK_UNAVAILABLE;

/**
 * Fraud detection orchestrator delegating to modular FraudCheck implementations.
 * Pure binary decisioning: SAFE (Approve) vs FRAUD (Reject & Block).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService implements FraudDetector {

    private final List<FraudCheck> fraudChecks;

    @Override
    public DetectionResult detect(Event.PaymentRequest request) {
        return evaluatePayment(request);
    }

    private DetectionResult evaluatePayment(Event.PaymentRequest request) {
        try {
            validate(request);
            RiskResult risk = evaluateChecks(request);
            if (risk.fraud()) {
                log.warn("FRAUD: txnId: {} sender: {} receiver: {} score: {} reasons: {}",
                        request.txnId(), MaskingUtil.maskAccountNo(request.senderAccountNo()),
                        MaskingUtil.maskAccountNo(request.receiverAccountNo()), risk.score(), risk.reasons());
                return rejected(request, FraudStatus.FRAUD, risk.score(), risk.reasons());
            }
            log.info("SAFE: txnId: {} sender: {} score: {}", request.txnId(), MaskingUtil.maskAccountNo(request.senderAccountNo()), risk.score());
            return approved(request, risk.score());
        } catch (InvalidPaymentException ex) {
            log.warn("Invalid payment: {}", ex.getMessage());
            if (request == null || isBlank(request.txnId())) return buildInvalidRequestResult(List.of(ex.getMessage()));
            return rejected(request, FraudStatus.ERROR, 0, List.of(ex.getMessage()));
        } catch (FraudException ex) {
            log.error("Fraud domain exception for txnId: {} sender: {}: {}",
                    request != null ? request.txnId() : "unknown",
                    request != null ? MaskingUtil.maskAccountNo(request.senderAccountNo()) : "unknown",
                    ex.getMessage());
            return rejected(request, FraudStatus.ERROR, 0, List.of(ex.getMessage()));
        } catch (Exception ex) {
            log.error("Unexpected error for txnId: {} sender: {}: {}",
                    request != null ? request.txnId() : "unknown",
                    request != null ? MaskingUtil.maskAccountNo(request.senderAccountNo()) : "unknown",
                    ex.getMessage(), ex);
            return rejected(request, FraudStatus.ERROR, 0, List.of(REASON_CHECK_UNAVAILABLE));
        }
    }

    /**
     * Validates payment request payload. Throws InvalidPaymentException on validation error.
     */
    public void validate(Event.PaymentRequest request) {
        if (request == null || isBlank(request.txnId())) {
            throw new InvalidPaymentException("Missing or empty transaction id");
        }
        if (isBlank(request.senderAccountNo()) || isBlank(request.receiverAccountNo())) {
            throw new InvalidPaymentException("Missing sender or receiver account number");
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new InvalidPaymentException("Payment amount must be greater than zero");
        }
    }

    private RiskResult evaluateChecks(Event.PaymentRequest request) {
        int totalScore = 0;
        List<String> aggregatedReasons = new ArrayList<>();
        boolean isFraud = false;
        for (FraudCheck check : this.fraudChecks) {
            RiskResult result = check.check(request);
            totalScore += result.score();
            if (result.reasons() != null) aggregatedReasons.addAll(result.reasons());
            if (result.fraud()) isFraud = true;
        }
        FraudStatus status = isFraud ? FraudStatus.FRAUD : FraudStatus.SAFE;
        String action = isFraud ? ACTION_REJECT_BLOCK : ACTION_APPROVE;
        return RiskResult.builder()
                .status(status)
                .fraud(isFraud)
                .score(totalScore)
                .reasons(List.copyOf(aggregatedReasons))
                .action(action)
                .build();
    }

    private DetectionResult rejected(
            Event.PaymentRequest request, FraudStatus status,
            int score, List<String> reasons
    ) {
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId(request.txnId() + ":" + status.name().toLowerCase(Locale.ROOT))
                        .txnId(request.txnId())
                        .senderAccountNo(request.senderAccountNo())
                        .receiverAccountNo(request.receiverAccountNo())
                        .amount(request.amount())
                        .accountType(request.accountType())
                        .riskScore(score)
                        .reasons(reasons)
                        .action(FraudConstant.ACTION_REJECT_BLOCK)
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return DetectionResult.builder()
                .status(status)
                .txnId(request.txnId())
                .key(request.senderAccountNo())
                .ack(ack)
                .action(FraudConstant.ACTION_REJECT_BLOCK)
                .riskScore(score)
                .build();
    }

    private DetectionResult approved(Event.PaymentRequest request, int score) {
        Event ack = Event.builder()
                .approved(Event.PaymentApproved.builder()
                        .txnId(request.txnId())
                        .senderAccountNo(request.senderAccountNo())
                        .receiverAccountNo(request.receiverAccountNo())
                        .amount(request.amount())
                        .accountType(request.accountType())
                        .riskScore(score)
                        .approvedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return DetectionResult.builder()
                .status(FraudStatus.SAFE)
                .txnId(request.txnId())
                .key(request.senderAccountNo())
                .ack(ack)
                .action(ACTION_APPROVE)
                .riskScore(score)
                .build();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private DetectionResult buildInvalidRequestResult(List<String> reasons) {
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId("unknown:error")
                        .txnId("unknown")
                        .senderAccountNo("unknown")
                        .receiverAccountNo("unknown")
                        .amount(BigDecimal.ZERO)
                        .accountType("UNKNOWN")
                        .riskScore(0)
                        .reasons(reasons)
                        .action(ACTION_REJECT_BLOCK)
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return DetectionResult.builder()
                .status(FraudStatus.ERROR)
                .txnId("unknown")
                .key("unknown")
                .ack(ack)
                .action(ACTION_REJECT_BLOCK)
                .riskScore(0)
                .build();
    }
}