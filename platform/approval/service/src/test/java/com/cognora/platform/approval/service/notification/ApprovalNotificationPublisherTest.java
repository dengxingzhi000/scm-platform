package com.cognora.platform.approval.service.notification;

import com.cognora.platform.approval.domain.entity.SysPermissionApproval;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.notify.api.request.NotificationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApprovalNotificationPublisherTest {

    private static final String TOPIC = "scm.notify.events";
    private static final UUID TENANT = UUID.fromString("019a0aee-0000-7000-8000-000000000002");

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final ApprovalNotificationPublisher publisher =
            new ApprovalNotificationPublisher(kafkaTemplate, TOPIC);

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    private SysPermissionApproval approval() {
        return new SysPermissionApproval()
                .setId("ap-1")
                .setApplicantId("u-1")
                .setApproverName("张三");
    }

    private NotificationRequest captureSent() {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("ap-1"), payload.capture());
        return (NotificationRequest) payload.getValue();
    }

    private void brokerAccepts() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(null));
    }

    @Test
    void tellsTheApplicantTheRequestWasApproved() {
        brokerAccepts();
        TenantContextHolder.setTenantId(TENANT);

        assertThat(publisher.publish(approval(), true)).isTrue();

        NotificationRequest request = captureSent();
        assertThat(request.getTenantId()).isEqualTo(TENANT.toString());
        assertThat(request.getUserId()).isEqualTo("u-1");
        assertThat(request.getNotifyType()).isEqualTo("APPROVAL");
        assertThat(request.getTitle()).isEqualTo("审批通过");
        assertThat(request.getContent()).contains("张三");
        assertThat(request.getLink()).isEqualTo("/approval");
        assertThat(request.getBusinessKey()).isEqualTo("approval:ap-1:approved");
    }

    @Test
    void carriesTheRejectReason() {
        brokerAccepts();
        TenantContextHolder.setTenantId(TENANT);

        assertThat(publisher.publish(approval().setRejectReason("预算不足"), false)).isTrue();

        NotificationRequest request = captureSent();
        assertThat(request.getTitle()).isEqualTo("审批驳回");
        assertThat(request.getContent()).contains("预算不足");
        assertThat(request.getBusinessKey()).isEqualTo("approval:ap-1:rejected");
    }

    @Test
    void refusesToGuessATenant() {
        TenantContextHolder.clear();

        assertThat(publisher.publish(approval(), true)).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void skipsARequestWithoutAnApplicant() {
        TenantContextHolder.setTenantId(TENANT);

        assertThat(publisher.publish(approval().setApplicantId(null), true)).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
