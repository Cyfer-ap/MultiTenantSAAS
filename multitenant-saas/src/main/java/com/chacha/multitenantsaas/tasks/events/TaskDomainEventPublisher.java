package com.chacha.multitenantsaas.tasks.events;

public interface TaskDomainEventPublisher {

    void publish(TaskDomainEvent event);
}
