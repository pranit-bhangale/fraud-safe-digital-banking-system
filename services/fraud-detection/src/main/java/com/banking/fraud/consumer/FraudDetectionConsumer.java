package com.banking.fraud.consumer;

import com.banking.fraud.client.KafkaClient;
import com.banking.fraud.constant.FraudConstant;
import com.banking.fraud.detection.FraudDetector;
import com.banking.fraud.dto.DetectionResult;
import com.banking.fraud.event.Event;
import com.banking.fraud.model.FraudStatus;
import com.banking.fraud.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

import static com.banking.fraud.constant.FraudConstant.ACTION_REJECT_BLOCK;
import static com.banking.fraud.constant.FraudConstant.HEADER_FRAUD_ACTION;
import static com.banking.fraud.constant.FraudConstant.HEADER_FRAUD_STATUS;
import static com.banking.fraud.constant.FraudConstant.HEADER_RISK_SCORE;
import static com.banking.fraud.constant.FraudConstant.HEADER_SENDER_ACCOUNT;
import static com.banking.fraud.constant.FraudConstant.HEADER_TXN_ID;
import static com.banking.fraud.constant.FraudConstant.REASON_INVALID_REQUEST;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FraudDetectionConsumer {

    private final KafkaClient kafkaClient;

    @Bean(name = {"fraudDetection"})
    public Function<Message<Event>, Message<Event>> fraudDetection(FraudDetector fraudDetector) {
        return inboundMessage -> {
            Event event = inboundMessage != null ? inboundMessage.getPayload() : null;
            Event.PaymentRequest request = event != null ? event.request() : null;
            if (request == null || isBlank(request.txnId())) {
                log.warn("Dropped inbound message: missing payment request or txnId");
                return buildErrorAck("unknown", "unknown", List.of(REASON_INVALID_REQUEST));
            }
            try {
                log.info("Evaluating payment: txnId: {} sender: {} receiver: {} amount: {} accountType: {}",
                        request.txnId(), MaskingUtil.maskAccountNo(request.senderAccountNo()),
                        MaskingUtil.maskAccountNo(request.receiverAccountNo()),
                        request.amount(), request.accountType()
                );
                DetectionResult result = fraudDetector.detect(request);
                if (result.status() == FraudStatus.FRAUD) {
                    this.kafkaClient.sendAsync(FraudConstant.TOPIC_FRAUD_ALERTS, result.key(), result.ack());
                }
                return buildAckMessage(result);
            } catch (Exception ex) {
                log.error("Exception during fraud evaluation for txnId: {} sender: {}: {}",
                        request.txnId(), MaskingUtil.maskAccountNo(request.senderAccountNo()), ex.getMessage(), ex);
                return buildErrorAck(request.txnId(), request.senderAccountNo(), List.of(ex.getMessage()));
            }
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private Message<Event> buildErrorAck(String txnId, String senderAccountNo, List<String> reasons) {
        String safeTxnId = !isBlank(txnId) ? txnId : "unknown";
        String safeSender = !isBlank(senderAccountNo) ? senderAccountNo : "unknown";
        Event ack = Event.builder()
                .event(Event.FraudEvent.builder()
                        .eventId(safeTxnId + ":error")
                        .txnId(safeTxnId)
                        .senderAccountNo(safeSender)
                        .receiverAccountNo("unknown")
                        .amount(BigDecimal.ZERO)
                        .accountType("UNKNOWN")
                        .riskScore(0)
                        .reasons(reasons)
                        .action(ACTION_REJECT_BLOCK)
                        .detectedAtMs(System.currentTimeMillis())
                        .build())
                .build();
        return MessageBuilder.withPayload(ack)
                .setHeader(KafkaHeaders.KEY, safeSender.getBytes(StandardCharsets.UTF_8))
                .setHeader(HEADER_FRAUD_STATUS, FraudStatus.ERROR.name())
                .setHeader(HEADER_FRAUD_ACTION, ACTION_REJECT_BLOCK)
                .setHeader(HEADER_RISK_SCORE, 0)
                .setHeader(HEADER_TXN_ID, safeTxnId)
                .setHeader(HEADER_SENDER_ACCOUNT, safeSender)
                .setHeader(KafkaClient.HEADER_TIMESTAMP, System.currentTimeMillis())
                .setHeader(KafkaClient.HEADER_SOURCE_SERVICE, "fraud-detection")
                .build();
    }

    private Message<Event> buildAckMessage(DetectionResult result) {
        MessageBuilder<Event> builder = MessageBuilder.withPayload(result.ack());
        if (result.key() != null && !result.key().isBlank()) {
            builder.setHeader(KafkaHeaders.KEY, result.key().getBytes(StandardCharsets.UTF_8));
        }
        builder.setHeader(HEADER_FRAUD_STATUS, result.status().name());
        builder.setHeader(HEADER_FRAUD_ACTION, result.action());
        builder.setHeader(HEADER_RISK_SCORE, result.riskScore());
        builder.setHeader(HEADER_TXN_ID, result.txnId());
        builder.setHeader(HEADER_SENDER_ACCOUNT, result.key());
        builder.setHeader(KafkaClient.HEADER_TIMESTAMP, System.currentTimeMillis());
        builder.setHeader(KafkaClient.HEADER_SOURCE_SERVICE, "fraud-detection");
        return builder.build();
    }
}