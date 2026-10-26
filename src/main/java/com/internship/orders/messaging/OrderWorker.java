package com.internship.orders.messaging;

import com.internship.orders.config.KafkaConfig;
import com.internship.orders.config.RabbitConfig;
import com.internship.orders.model.OrderEntity;
import com.internship.orders.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Consumes orders from RabbitMQ, processes them, then publishes an event to Kafka. */
@Component
public class OrderWorker {

    private static final Logger log = LoggerFactory.getLogger(OrderWorker.class);

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderWorker(OrderRepository orderRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void processOrder(String orderId) throws InterruptedException {
        OrderEntity order = orderRepository.findById(Long.parseLong(orderId)).orElse(null);

        // Idempotent: a redelivered message never processes an order twice
        if (order == null || !"PENDING".equals(order.getStatus())) {
            log.warn("Skipping order {} (missing or already processed)", orderId);
            return;
        }

        Thread.sleep(2000); // simulate heavy work: stock check, payment, etc.

        order.setStatus("PROCESSED");
        orderRepository.save(order);
        log.info("Order {} processed", orderId);

        // Publish event to Kafka (key = orderId keeps events for one order in the same partition)
        String json = String.format(
                "{\"orderId\":%s,\"productId\":\"%s\",\"quantity\":%d,\"status\":\"PROCESSED\"}",
                orderId, order.getProductId(), order.getQuantity());
        kafkaTemplate.send(KafkaConfig.TOPIC, orderId, json);
    }
}
