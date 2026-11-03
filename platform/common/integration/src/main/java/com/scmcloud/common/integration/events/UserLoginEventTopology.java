package com.scmcloud.common.integration.events;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserLoginEventTopology {

    @Bean
    public DirectExchange userLoginExchange() {
        return new DirectExchange(UserLoginEventChannels.EXCHANGE, true, false);
    }

    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable(UserLoginEventChannels.QUEUE).build();
    }

    @Bean
    public Binding userLoginBinding(Queue userLoginQueue, DirectExchange userLoginExchange) {
        return BindingBuilder.bind(userLoginQueue)
                .to(userLoginExchange)
                .with(UserLoginEventChannels.ROUTING_KEY);
    }
}
