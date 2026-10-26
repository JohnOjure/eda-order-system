package com.internship.orders.messaging;

import com.internship.orders.config.KafkaConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Kafka consumer group #1: pushes real-time updates to the browser over WebSocket. */
@Component
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = KafkaConfig.TOPIC, groupId = "notification-group")
    public void onOrderEvent(ConsumerRecord<String, String> record) {
        messagingTemplate.convertAndSend("/topic/orders/" + record.key(), record.value());
    }
}
