package com.banking.fraud.detection;

import com.banking.fraud.client.RedisLuaClient;
import com.banking.fraud.dto.DetectionResult;
import com.banking.fraud.dto.RiskResult;
import com.banking.fraud.event.Event;
import com.banking.fraud.model.FraudStatus;
import com.banking.fraud.properties.FraudProperties;
import com.banking.fraud.properties.FraudProperties.Weights;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_CARD;
import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_DEVICE;
import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_IP;
import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_USER;
import static com.banking.fraud.constant.FraudConstant.KEY_WINDOW_USER;
import static com.banking.fraud.constant.FraudConstant.METRIC_DECISIONS;
import static com.banking.fraud.constant.FraudConstant.METRIC_DURATION;
import static com.banking.fraud.constant.FraudConstant.REASON_BURST;
import static com.banking.fraud.constant.FraudConstant.REASON_CARD_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.REASON_CHECK_UNAVAILABLE;
import static com.banking.fraud.constant.FraudConstant.REASON_DEVICE_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.REASON_HIGH_AMOUNT;
import static com.banking.fraud.constant.FraudConstant.REASON_INVALID_REQUEST;
import static com.banking.fraud.constant.FraudConstant.REASON_IP_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.REASON_USER_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.SCRIPT_VELOCITY_CHECK;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService implements FraudDetector {

    /**
     * Velocity check reasons in order matching Lua script flags.
     */
    private static final String[] VELOCITY_REASONS = {
            REASON_USER_VELOCITY,
            REASON_CARD_VELOCITY,
            REASON_DEVICE_VELOCITY,
            REASON_IP_VELOCITY,
            REASON_BURST
    };
    private final RedisLuaClient lua;
    private final FraudProperties props;
    private final MeterRegistry meterRegistry;

    @Override
    public DetectionResult detect(Event.PaymentRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        DetectionResult result = doDetect(request);
        String status = result.status().name();
        sample.stop(meterRegistry.timer(METRIC_DURATION, "status", status));
        meterRegistry.counter(METRIC_DECISIONS, "status", status).increment();
        return result;
    }

    private DetectionResult doDetect(Event.PaymentRequest r) {
        if (isBlank(r.txnId())) {
            log.warn("Missing txnId in payment request");
            return rejectedWithTxnId(List.of(REASON_INVALID_REQUEST));
        }
        if (isBlank(r.userId()) || r.amount() == null || r.amount().signum() <= 0) {
            log.warn("txn={} invalid payment request", r.txnId());
            return rejected(r, FraudStatus.ERROR, 0, List.of(REASON_INVALID_REQUEST));
        }

        try {
            RiskResult risk = evaluateRisk(r);
            if (risk.fraud()) {
                log.warn("txn={} user={} FRAUD score={} reasons={}",
                        r.txnId(), r.userId(), risk.score(), risk.reasons());
                return rejected(r, FraudStatus.FRAUD, risk.score(), risk.reasons());
            }

            log.info("txn={} user={} SAFE score={}", r.txnId(), r.userId(), risk.score());
            return approved(r, risk.score());

        } catch (Exception e) {
            log.error("txn={} fraud check failed, answering ERROR", r.txnId(), e);
            return rejected(r, FraudStatus.ERROR, 0, List.of(REASON_CHECK_UNAVAILABLE));
        }
    }

    /**
     * Evaluates fraud risk using Redis Lua script for atomic velocity checks.
     */
    private RiskResult evaluateRisk(Event.PaymentRequest p) {
        List<String> keys = List.of(
                KEY_LOCK_USER + p.userId(),
                KEY_LOCK_CARD + sha256(orUnique(p.cardToken(), p.txnId())),
                KEY_LOCK_DEVICE + orUnique(p.deviceId(), p.txnId()),
                KEY_LOCK_IP + orUnique(p.ipAddress(), p.txnId()),
                KEY_WINDOW_USER + p.userId()
        );

        List<Long> flags = lua.executeForLongList(
                SCRIPT_VELOCITY_CHECK,
                keys,
                p.txnId(),
                props.lockTtlSeconds(),
                props.windowMs(),
                props.windowLimit()
        );

        if (flags == null || flags.size() != VELOCITY_REASONS.length) {
            throw new IllegalStateException("Unexpected result from " + SCRIPT_VELOCITY_CHECK + ": " + flags);
        }

        Weights w = props.weights();
        int[] weights = {
                w.userVelocity(),
                w.cardVelocity(),
                w.deviceVelocity(),
                w.ipVelocity(),
                w.burst()
        };

        int score = 0;
        List<String> reasons = new ArrayList<>();
        for (int i = 0; i < VELOCITY_REASONS.length; i++) {
            if (flags.get(i) == 1L) {
                score += weights[i];
                reasons.add(VELOCITY_REASONS[i]);
            }
        }

        if (p.amount().compareTo(props.highAmount()) >= 0) {
            score += w.highAmount();
            reasons.add(REASON_HIGH_AMOUNT);
        }

        return RiskResult.builder()
                .fraud(score >= props.threshold())
                .score(score)
                .reasons(List.copyOf(reasons))
                .build();
    }

    private String partitionKey(Event.PaymentRequest r) {
        return r.txnId();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String orUnique(String value, String txnId) {
        return (value == null || value.isBlank()) ? "unknown:" + txnId : value;
    }

    private String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private DetectionResult approved(Event.PaymentRequest r, int score) {
        Event ack = Event.builder()
                .approved(Event.PaymentApproved.builder()
                        .txnId(r.txnId())
                        .userId(r.userId())
                        .amount(r.amount())
                        .currency(r.currency())
                        .merchantId(r.merchantId())
                        .riskScore(score)
                        .approvedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return new DetectionResult(FraudStatus.SAFE, partitionKey(r), ack);
    }

    private DetectionResult rejected(Event.PaymentRequest r, FraudStatus status, int score, List<String> reasons) {
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId(r.txnId() + ":" + status.name().toLowerCase(Locale.ROOT))
                        .txnId(r.txnId())
                        .userId(r.userId())
                        .amount(r.amount())
                        .currency(r.currency())
                        .riskScore(score)
                        .reasons(reasons)
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return new DetectionResult(status, partitionKey(r), ack);
    }

    private DetectionResult rejectedWithTxnId(List<String> reasons) {
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId("unknown" + ":" + FraudStatus.ERROR.name().toLowerCase(Locale.ROOT))
                        .txnId(null)
                        .userId("unknown")
                        .amount(java.math.BigDecimal.ZERO)
                        .currency("UNKNOWN")
                        .riskScore(0)
                        .reasons(reasons)
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return new DetectionResult(FraudStatus.ERROR, "unknown", ack);
    }
}