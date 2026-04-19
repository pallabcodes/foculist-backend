package com.yourorg.platform.foculist.resource.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitResilienceConfig {

    public static final String DLX_NAME = "foculist.resource.dlx";
    public static final String DLQ_NAME = "foculist.resource.dlq";
    public static final String PROMOTED_WORKLOGS_QUEUE = "foculist.resource.promoted-worklogs";
    public static final String MEETING_EXCHANGE = "foculist.meeting.events";

    @Bean
    public TopicExchange meetingExchange() {
        return new TopicExchange(MEETING_EXCHANGE);
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
    public Queue promotedWorklogsQueue() {
        return QueueBuilder.durable(PROMOTED_WORKLOGS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", "deadLetter")
                .build();
    }

    @Bean
    public Binding promotedWorklogsBinding() {
        return BindingBuilder.bind(promotedWorklogsQueue()).to(meetingExchange()).with("meeting.worklog.promoted");
    }
}
