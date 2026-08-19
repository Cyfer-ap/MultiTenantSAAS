package com.chacha.multitenantsaas.service;

import com.chacha.multitenantsaas.dto.NotificationDeliveryTask;
import com.chacha.multitenantsaas.entity.NotificationDeliveryChannel;

public interface NotificationDeliveryHandler {
    NotificationDeliveryChannel channel();

    void deliver(NotificationDeliveryTask task);
}
