package com.cognora.platform.approval.service.notification;

import com.cognora.platform.approval.domain.entity.SysPermissionApproval;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.notify.api.request.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Tells the applicant that their request was decided. Published rather than called so a
 * notification outage can never fail or slow down an approval.
 */
@Slf4j
@Component
public class ApprovalNotificationPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public ApprovalNotificationPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${notify.request-topic:cognora.notify.events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public boolean publish(SysPermissionApproval approval, boolean approved) {
        String applicantId = approval.getApplicantId();
        UUID tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(applicantId) || tenantId == null) {
            log.debug("Skip approval notification: approvalId={}, applicantId={}, tenant={}",
                    approval.getId(), applicantId, tenantId);
            return false;
        }

        NotificationRequest request = new NotificationRequest()
                .setTenantId(tenantId.toString())
                .setUserId(applicantId)
                .setNotifyType("APPROVAL")
                .setTitle(approved ? "审批通过" : "审批驳回")
                .setContent(content(approval, approved))
                .setLink("/approval")
                .setBusinessKey("approval:" + approval.getId() + (approved ? ":approved" : ":rejected"));
        try {
            kafkaTemplate.send(topic, approval.getId(), request).get(5, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing approval notification {}", approval.getId());
            return false;
        } catch (Exception e) {
            log.warn("Failed to publish approval notification {}", approval.getId(), e);
            return false;
        }
    }

    private String content(SysPermissionApproval approval, boolean approved) {
        String who = StringUtils.hasText(approval.getApproverName())
                ? approval.getApproverName()
                : "审批人";
        if (approved) {
            return "您提交的权限申请已由 " + who + " 审批通过";
        }
        String reason = StringUtils.hasText(approval.getRejectReason())
                ? "，原因：" + approval.getRejectReason()
                : "";
        return "您提交的权限申请已被 " + who + " 驳回" + reason;
    }
}
