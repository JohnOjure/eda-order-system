package com.internship.orders.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE = "orderQueue";
    public static final String EXCHANGE = "orderExchange";
    public static final String ROUTING_KEY = "orderRoutingKey";

    @Bean public Queue orderQueue() { return new Queue(QUEUE, true); }
    @Bean public TopicExchange orderExchange() { return new TopicExchange(EXCHANGE); }
    @Bean public Binding orderBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ROUTING_KEY);
    }
}
