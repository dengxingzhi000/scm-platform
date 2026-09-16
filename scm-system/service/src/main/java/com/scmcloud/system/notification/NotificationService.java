package com.scmcloud.system.notification;

import com.scmcloud.system.notification.model.NotificationChannel;
import com.scmcloud.system.notification.model.NotificationCommand;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Central notification orchestration service (email, in-app, etc).
 */
public interface NotificationService {

    /**
     * 鍙戦€侀€氱煡锛堝畬鏁村懡浠わ級
     */
    void send(NotificationCommand command);

    /**
     * 鍙戦€侀€氱煡锛堜究鎹锋柟娉曪級
     *
     * @param username     鐢ㄦ埛鍚嶏紙鐢ㄤ簬绔欏唴娑堟伅锟?
     * @param email        閭锛堢敤浜庨偖浠堕€氱煡锟?
     * @param templateCode 妯℃澘缂栫爜
     * @param subject      涓婚
     * @param variables    妯℃澘鍙橀噺
     */
    default void sendNotification(String username, String email, String templateCode,
                                  String subject, Map<String, Object> variables) {
        NotificationCommand.NotificationCommandBuilder builder = NotificationCommand.builder()
                .referenceId(UUID.randomUUID().toString())
                .templateCode(templateCode)
                .subject(subject)
                .username(username)
                .email(email);

        if (variables != null) {
            variables.forEach(builder::variable);
        }

        // 榛樿鍙戦€佺珯鍐呮秷鎭拰閭欢
        builder.channel(NotificationChannel.SYSTEM_MESSAGE);
        if (email != null && !email.isEmpty()) {
            builder.channel(NotificationChannel.EMAIL);
        }

        send(builder.build());
    }

    /**
     * 鍙戦€侀€氱煡鍒版寚瀹氭笭锟?
     */
    default void sendNotification(String username, String email, String templateCode,
                                  String subject, Map<String, Object> variables,
                                  Set<NotificationChannel> channels) {
        NotificationCommand.NotificationCommandBuilder builder = NotificationCommand.builder()
                .referenceId(UUID.randomUUID().toString())
                .templateCode(templateCode)
                .subject(subject)
                .username(username)
                .email(email);

        if (variables != null) {
            variables.forEach(builder::variable);
        }

        if (channels != null) {
            channels.forEach(builder::channel);
        }

        send(builder.build());
    }
}

