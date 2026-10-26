package com.internship.orders.controller;

import com.internship.orders.messaging.AuditConsumer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final AuditConsumer auditConsumer;

    public StatsController(AuditConsumer auditConsumer) {
        this.auditConsumer = auditConsumer;
    }

    @GetMapping
    public Map<String, Long> stats() {
        return Map.of("processedOrderEvents", auditConsumer.getProcessedCount());
    }
}
