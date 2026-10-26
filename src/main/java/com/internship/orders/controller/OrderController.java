package com.internship.orders.controller;

import com.internship.orders.model.OrderEntity;
import com.internship.orders.repository.OrderRepository;
import com.internship.orders.dto.OrderRequest;
import com.internship.orders.config.RabbitConfig;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository;

    // One token bucket per client IP: 5 requests per minute
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    // Idempotency-Key -> order id (prevents duplicate orders on double-click/retry)
    private final Map<String, Long> idempotencyKeys = new ConcurrentHashMap<>();

    public OrderController(RabbitTemplate rabbitTemplate, OrderRepository orderRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.orderRepository = orderRepository;
    }

    private Bucket bucketFor(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build());
    }

    @PostMapping
    public ResponseEntity<String> placeOrder(
            @RequestBody OrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            HttpServletRequest http) {

        // 1. Rate limiting (per IP)
        if (!bucketFor(http.getRemoteAddr()).tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many requests! Slow down.");
        }

        // 2. Idempotency: same key => return the original order, do nothing new
        if (idemKey != null && idempotencyKeys.containsKey(idemKey)) {
            return ResponseEntity.accepted().body("Duplicate request ignored. Tracking ID: " + idempotencyKeys.get(idemKey));
        }

        // 3. Save as PENDING
        OrderEntity order = new OrderEntity();
        order.setProductId(request.productId());
        order.setQuantity(request.quantity());
        order.setStatus("PENDING");
        orderRepository.save(order);
        if (idemKey != null) idempotencyKeys.put(idemKey, order.getId());

        // 4. Hand off to RabbitMQ work queue
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ROUTING_KEY, String.valueOf(order.getId()));

        // 5. Respond immediately
        return ResponseEntity.accepted().body("Order received! Tracking ID: " + order.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderEntity> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
