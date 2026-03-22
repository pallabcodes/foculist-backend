package com.yourorg.platform.foculist.project.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "foculist.workspace.events";
    public static final String QUEUE_NAME = "foculist.project.workspace.queue";
    public static final String ROUTING_KEY = "workspace.created";

    @Bean
    public TopicExchange workspaceExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue projectWorkspaceQueue() {
        return new Queue(QUEUE_NAME, true); // durable
    }

    @Bean
    public Binding binding(Queue projectWorkspaceQueue, TopicExchange workspaceExchange) {
        return BindingBuilder.bind(projectWorkspaceQueue).to(workspaceExchange).with(ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
