package com.banking.fraud.detection;

import com.banking.fraud.client.RedisLuaClient;
import com.banking.fraud.dto.RiskResult;
import com.banking.fraud.event.Event;
import com.banking.fraud.exception.VelocityEvaluationException;
import com.banking.fraud.model.FraudStatus;
import com.banking.fraud.properties.FraudProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.banking.fraud.constant.FraudConstant.ACTION_APPROVE;
import static com.banking.fraud.constant.FraudConstant.ACTION_REJECT_BLOCK;
import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_RECEIVER;
import static com.banking.fraud.constant.FraudConstant.KEY_LOCK_SENDER;
import static com.banking.fraud.constant.FraudConstant.KEY_WINDOW_SENDER;
import static com.banking.fraud.constant.FraudConstant.REASON_BURST;
import static com.banking.fraud.constant.FraudConstant.REASON_RECEIVER_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.REASON_SENDER_VELOCITY;
import static com.banking.fraud.constant.FraudConstant.SCRIPT_VELOCITY_CHECK;

/**
 * Evaluates velocity-only fraud checks atomically via Redis Lua script.
 * Verified against transactionId and partitioned by senderAccountNo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VelocityCheck implements FraudCheck {

    private static final String[] VELOCITY_REASONS = {
            REASON_SENDER_VELOCITY,
            REASON_RECEIVER_VELOCITY,
            REASON_BURST
    };

    private final RedisLuaClient luaClient;
    private final FraudProperties properties;

    @Override
    public RiskResult check(Event.PaymentRequest request) {
        String sender = request.senderAccountNo();
        String receiver = request.receiverAccountNo();
        String txnId = request.txnId();
        List<String> keys = List.of(
                String.format(KEY_LOCK_SENDER, sender),
                String.format(KEY_LOCK_RECEIVER, sender, receiver),
                String.format(KEY_WINDOW_SENDER, sender)
        );
        List<Long> flags = this.luaClient.executeForLongList(
                SCRIPT_VELOCITY_CHECK, keys, txnId,
                this.properties.lockTtlSeconds(),
                this.properties.windowMs(), this.properties.windowLimit()
        );
        if (flags == null || flags.size() != VELOCITY_REASONS.length) {
            throw new VelocityEvaluationException("Unexpected velocity check flags from Redis: " + flags);
        }
        FraudProperties.Weights w = this.properties.weights();
        int[] weights = {
                w.senderVelocity(),
                w.receiverVelocity(),
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
        boolean isFraud = score >= this.properties.threshold();
        return RiskResult.builder()
                .status(isFraud ? FraudStatus.FRAUD : FraudStatus.SAFE)
                .fraud(isFraud)
                .score(score)
                .reasons(List.copyOf(reasons))
                .action(isFraud ? ACTION_REJECT_BLOCK : ACTION_APPROVE)
                .build();
    }
}