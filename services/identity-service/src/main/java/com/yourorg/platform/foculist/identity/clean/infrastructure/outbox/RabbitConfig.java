package com.yourorg.platform.foculist.identity.clean.infrastructure.outbox;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "foculist.workspace.events";

    @Bean
    public org.springframework.amqp.core.TopicExchange workspaceExchange() {
        return new org.springframework.amqp.core.TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
