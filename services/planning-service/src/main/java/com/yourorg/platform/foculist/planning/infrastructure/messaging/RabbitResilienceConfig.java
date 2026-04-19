package com.yourorg.platform.foculist.planning.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitResilienceConfig {

    public static final String DLX_NAME = "foculist.planning.dlx";
    public static final String DLQ_NAME = "foculist.planning.dlq";
    public static final String PROMOTED_TASKS_QUEUE = "foculist.planning.promoted-tasks";
    public static final String MEETING_EXCHANGE = "foculist.meeting.events";

    @Bean
    public org.springframework.amqp.core.TopicExchange meetingExchange() {
        return new org.springframework.amqp.core.TopicExchange(MEETING_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_NAME).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with("deadLetter");
    }

    @Bean
    public Queue promotedTasksQueue() {
        return QueueBuilder.durable(PROMOTED_TASKS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    public Binding promotedTasksBinding() {
        return BindingBuilder.bind(promotedTasksQueue()).to(meetingExchange()).with("meeting.task.promoted");
    }
}
