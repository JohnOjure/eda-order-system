package com.internship.orders.messaging;

import com.internship.orders.config.KafkaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/** Kafka consumer group #2: independently receives the SAME events (audit/analytics). */
@Component
public class AuditConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditConsumer.class);
    private final AtomicLong processedCount = new AtomicLong();

    @KafkaListener(topics = KafkaConfig.TOPIC, groupId = "audit-group")
    public void audit(String message) {
        long n = processedCount.incrementAndGet();
        log.info("[AUDIT] event #{}: {}", n, message);
    }

    public long getProcessedCount() { return processedCount.get(); }
}
