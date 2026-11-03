package com.cognora.platform.system.notification.channel;

import com.cognora.platform.system.notification.model.NotificationChannel;
import com.cognora.platform.system.notification.model.NotificationCommand;

/**
 * SPI for channel-specific notification delivery.
 */
public interface ChannelNotifier {

    NotificationChannel channel();

    void send(NotificationCommand command);
}

