package com.booknest.order.event;

import com.booknest.order.dto.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

// Producer for sending order-related events to Kafka
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEventDto> kafkaTemplate;

    // Kafka topic name for order events
    private static final String TOPIC = "order-events";

    // Build and send an order event to the Kafka topic
    public void sendOrderEvent(OrderEventDto event) {
        try {
            kafkaTemplate.send(TOPIC, event);
            log.info("Order event sent to Kafka: type={}, userId={}", event.getType(), event.getUserId());
        } catch (Exception e) {
            log.warn("Failed to send Kafka event: {}", e.getMessage());
        }
    }
}