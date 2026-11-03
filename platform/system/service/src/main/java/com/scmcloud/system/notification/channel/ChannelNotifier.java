package com.scmcloud.system.notification.channel;

import com.scmcloud.system.notification.model.NotificationChannel;
import com.scmcloud.system.notification.model.NotificationCommand;

/**
 * SPI for channel-specific notification delivery.
 */
public interface ChannelNotifier {

    NotificationChannel channel();

    void send(NotificationCommand command);
}

