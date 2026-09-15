package com.chacha.multitenantsaas.tasks.events;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringTaskDomainEventPublisher implements TaskDomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringTaskDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(TaskDomainEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
