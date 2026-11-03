package com.cognora.platform.approval.service.command;

import com.cognora.platform.approval.domain.entity.SysPermissionApproval;
import com.cognora.platform.approval.mapper.SysPermissionApprovalMapper;
import com.cognora.platform.approval.service.notification.ApprovalNotificationPublisher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysPermissionApprovalCommandServiceTest {

    private final SysPermissionApprovalMapper mapper = mock(SysPermissionApprovalMapper.class);
    private final ApprovalNotificationPublisher publisher = mock(ApprovalNotificationPublisher.class);
    private final SysPermissionApprovalCommandService service =
            new SysPermissionApprovalCommandService(mapper, publisher);

    private SysPermissionApproval pending() {
        return new SysPermissionApproval()
                .setId("ap-1")
                .setApplicantId("u-applicant")
                .setApprovalStatus(0);
    }

    @Test
    void notifiesTheApplicantAfterApproving() {
        SysPermissionApproval approval = pending();
        when(mapper.selectById("ap-1")).thenReturn(approval);
        when(mapper.updateById(any(SysPermissionApproval.class))).thenReturn(1);

        service.approve("ap-1", "approver-1", "张三");

        verify(publisher).publish(approval, true);
    }

    @Test
    void notifiesTheApplicantAfterRejecting() {
        SysPermissionApproval approval = pending();
        when(mapper.selectById("ap-1")).thenReturn(approval);
        when(mapper.updateById(any(SysPermissionApproval.class))).thenReturn(1);

        service.reject("ap-1", "approver-1", "张三", "资料不全");

        verify(publisher).publish(approval, false);
    }

    @Test
    void doesNotNotifyWhenTheStatusChangeFails() {
        SysPermissionApproval approval = pending();
        when(mapper.selectById("ap-1")).thenReturn(approval);
        when(mapper.updateById(any(SysPermissionApproval.class))).thenReturn(0);

        assertThatThrownBy(() -> service.approve("ap-1", "approver-1", "张三"))
                .isInstanceOf(RuntimeException.class);

        verify(publisher, never()).publish(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }
}
