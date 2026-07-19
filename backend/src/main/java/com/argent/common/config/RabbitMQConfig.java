package com.argent.common.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String WEBHOOK_QUEUE = "argent.webhooks";
    public static final String REPORTING_QUEUE = "argent.reporting";

    @Bean
    public Queue webhookQueue() {
        return new Queue(WEBHOOK_QUEUE, true);
    }

    @Bean
    public Queue reportingQueue() {
        return new Queue(REPORTING_QUEUE, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
