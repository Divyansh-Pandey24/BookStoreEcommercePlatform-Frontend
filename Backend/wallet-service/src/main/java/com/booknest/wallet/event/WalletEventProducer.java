package com.booknest.wallet.event;

import com.booknest.wallet.dto.WalletEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Producer for publishing wallet events to Kafka
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletEventProducer {

    private final KafkaTemplate<String, WalletEventDto> kafkaTemplate;
    private static final String TOPIC = "wallet-events";

    // Send wallet event to Kafka
    public void sendWalletEvent(WalletEventDto event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Wallet event published: type={}, userId={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to publish wallet event: {}", e.getMessage());
        }
    }
}