package com.scmcloud.notify.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.notify.domain.entity.SysUserNotification;
import com.scmcloud.notify.service.ISysUserNotificationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserNotificationControllerTest {

    private final ISysUserNotificationService service = mock(ISysUserNotificationService.class);
    private final SysUserNotificationController controller = new SysUserNotificationController(service);

    @Test
    void pageForwardsEveryFilterToTheService() {
        LocalDateTime from = LocalDateTime.of(2026, 10, 1, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 10, 31, 23, 59);
        when(service.pageList(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(new Page<SysUserNotification>());

        controller.page(2, 20, "ORDER", false, "退款", from, to);

        verify(service).pageList(2, 20, "ORDER", false, "退款", from, to);
    }

    @Test
    void deleteReturnsWhetherTheRowWasRemoved() {
        when(service.deleteNotification("n-1")).thenReturn(true);

        assertThat(controller.delete("n-1").data()).isTrue();
        verify(service).deleteNotification("n-1");
    }

    @Test
    void deleteBatchShortCircuitsOnAnEmptySelection() {
        assertThat(controller.deleteBatch(List.of()).data()).isZero();
        verify(service, never()).deleteNotifications(any());
    }

    @Test
    void deleteBatchRejectsAnOversizedSelection() {
        List<String> ids = IntStream.rangeClosed(1, 201).mapToObj(i -> "n-" + i).toList();

        assertThatThrownBy(() -> controller.deleteBatch(ids))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("200");
        verify(service, never()).deleteNotifications(any());
    }

    @Test
    void deleteBatchReturnsTheAffectedRowCount() {
        List<String> ids = List.of("n-1", "n-2");
        when(service.deleteNotifications(ids)).thenReturn(2);

        assertThat(controller.deleteBatch(ids).data()).isEqualTo(2);
    }
}
