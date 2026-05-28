package com.frog.notify.api;

import java.io.Serializable;
import java.util.List;

/**
 * 通知服务 Dubbo 接口
 *
 * <p>提供单条/批量通知发送等核心功能，供其他微服务通过 RPC 调用。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface NotifyDubboService {

    /**
     * 发送单条通知
     *
     * @param request 通知请求
     * @return 发送结果
     */
    NotifyResult sendNotification(NotificationRequest request);

    /**
     * 发送批量通知
     *
     * @param request 批量通知请求
     * @return 发送结果
     */
    BatchNotifyResult sendBatchNotification(BatchNotificationRequest request);

    /**
     * 通知请求
     */
    class NotificationRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long userId;
        private String notifyType;
        private String channel;
        private String title;
        private String content;
        private String templateCode;
        private java.util.Map<String, String> templateParams;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getNotifyType() {
            return notifyType;
        }

        public void setNotifyType(String notifyType) {
            this.notifyType = notifyType;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public java.util.Map<String, String> getTemplateParams() {
            return templateParams;
        }

        public void setTemplateParams(java.util.Map<String, String> templateParams) {
            this.templateParams = templateParams;
        }
    }

    /**
     * 批量通知请求
     */
    class BatchNotificationRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Long> userIds;
        private String notifyType;
        private String channel;
        private String title;
        private String content;
        private String templateCode;
        private java.util.Map<String, String> templateParams;

        public List<Long> getUserIds() {
            return userIds;
        }

        public void setUserIds(List<Long> userIds) {
            this.userIds = userIds;
        }

        public String getNotifyType() {
            return notifyType;
        }

        public void setNotifyType(String notifyType) {
            this.notifyType = notifyType;
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public java.util.Map<String, String> getTemplateParams() {
            return templateParams;
        }

        public void setTemplateParams(java.util.Map<String, String> templateParams) {
            this.templateParams = templateParams;
        }
    }

    /**
     * 通知发送结果
     */
    class NotifyResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long notifyId;
        private boolean success;
        private String message;

        public Long getNotifyId() {
            return notifyId;
        }

        public void setNotifyId(Long notifyId) {
            this.notifyId = notifyId;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * 批量通知发送结果
     */
    class BatchNotifyResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private int totalCount;
        private int successCount;
        private int failCount;
        private String message;

        public int getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(int totalCount) {
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public void setFailCount(int failCount) {
            this.failCount = failCount;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
