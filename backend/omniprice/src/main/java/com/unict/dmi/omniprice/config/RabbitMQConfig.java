package com.unict.dmi.omniprice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione RabbitMQ per la pipeline di notifiche alert.
 * Attivabile con: omniprice.rabbitmq.enabled=true in application.properties
 *
 * Pattern: Request Pipeline - i messaggi di alert scorrono dalla sorgente al consumatore
 * Pattern: Idempotent Receiver - il consumatore deduplicato tramite messageId nel DB
 * Pattern: Request Batch - le notifiche vengono aggregate prima di essere inviate
 *
 * Per avviare RabbitMQ con Docker:
 *   docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
 */
@Configuration
@ConditionalOnProperty(name = "omniprice.rabbitmq.enabled", havingValue = "true")
public class RabbitMQConfig {

    public static final String ALERT_QUEUE = "omniprice.alert.queue";
    public static final String ALERT_EXCHANGE = "omniprice.alerts";
    public static final String ALERT_ROUTING_KEY = "alert.triggered";

    public static final String BATCH_QUEUE = "omniprice.batch.queue";
    public static final String BATCH_ROUTING_KEY = "batch.write";

    @Bean
    public Queue alertQueue() {
        return QueueBuilder.durable(ALERT_QUEUE).build();
    }

    @Bean
    public Queue batchQueue() {
        return QueueBuilder.durable(BATCH_QUEUE).build();
    }

    @Bean
    public DirectExchange alertExchange() {
        return new DirectExchange(ALERT_EXCHANGE);
    }

    @Bean
    public Binding alertBinding(Queue alertQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(alertQueue).to(alertExchange).with(ALERT_ROUTING_KEY);
    }

    @Bean
    public Binding batchBinding(Queue batchQueue, DirectExchange alertExchange) {
        return BindingBuilder.bind(batchQueue).to(alertExchange).with(BATCH_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
