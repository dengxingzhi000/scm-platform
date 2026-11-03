package com.cognora.scm.purchase.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cognora.platform.common.exception.ServiceException;
import com.cognora.scm.purchase.domain.dto.PurRequestConvertDTO;
import com.cognora.scm.purchase.domain.entity.PurOrder;
import com.cognora.scm.purchase.domain.entity.PurOrderItem;
import com.cognora.scm.purchase.domain.entity.PurRequest;
import com.cognora.scm.purchase.domain.entity.PurRequestItem;
import com.cognora.scm.purchase.mapper.PurOrderItemMapper;
import com.cognora.scm.purchase.mapper.PurOrderMapper;
import com.cognora.scm.purchase.mapper.PurRequestItemMapper;
import com.cognora.scm.purchase.mapper.PurRequestMapper;
import com.cognora.scm.purchase.service.command.PurRequestCommandService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurRequestCommandServiceTest {

    private final PurRequestMapper requestMapper = mock(PurRequestMapper.class);
    private final PurRequestItemMapper requestItemMapper = mock(PurRequestItemMapper.class);
    private final PurOrderMapper orderMapper = mock(PurOrderMapper.class);
    private final PurOrderItemMapper orderItemMapper = mock(PurOrderItemMapper.class);
    private final PurRequestCommandService service = new PurRequestCommandService(
            requestMapper, requestItemMapper, orderMapper, orderItemMapper);

    @BeforeAll
    static void initLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, PurRequest.class);
        TableInfoHelper.initTableInfo(assistant, PurRequestItem.class);
    }

    private static PurRequest approvedRequest() {
        return new PurRequest()
                .setId("req-1")
                .setRequestNo("PR-20261030-0001")
                .setStatus(2)
                .setConverted(false)
                .setRequesterId("user-9")
                .setRequesterName("王申请")
                .setCurrentApproverId("user-8")
                .setCurrentApproverName("李审批")
                .setApprovedAt(LocalDateTime.of(2026, 10, 29, 9, 0))
                .setDeleted(false);
    }

    private static PurRequestItem line(String id, String name, String quantity, String budgetPrice) {
        return new PurRequestItem()
                .setId(id)
                .setRequestId("req-1")
                .setMaterialName(name)
                .setQuantity(new BigDecimal(quantity))
                .setUnit("件")
                .setBudgetPrice(budgetPrice == null ? null : new BigDecimal(budgetPrice));
    }

    private static PurRequestConvertDTO convertPayload() {
        return new PurRequestConvertDTO(
                "supplier-1", "测试供应商", "张经理",
                "warehouse-1", "主仓库", "上海市",
                1, LocalDateTime.of(2026, 11, 15, 18, 0),
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("0.13"),
                null,
                List.of(new PurRequestConvertDTO.ItemPrice("item-1", new BigDecimal("150"))));
    }

    @Test
    void convertCreatesOrderItemsAndMarksRequestConverted() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest());
        when(requestItemMapper.selectList(any())).thenReturn(List.of(
                line("item-1", "螺丝", "2", "100"),
                line("item-2", "垫片", "1", "100")));
        when(requestMapper.update(isNull(), any())).thenReturn(1);

        PurOrder order = service.convert("req-1", convertPayload(), "user-1", "张三");

        assertThat(order.getOrderNo()).startsWith("PO-");
        assertThat(order.getRequestId()).isEqualTo("req-1");
        assertThat(order.getRequestNo()).isEqualTo("PR-20261030-0001");
        assertThat(order.getStatus()).isEqualTo(2);
        assertThat(order.getSupplierName()).isEqualTo("测试供应商");
        assertThat(order.getBuyerId()).isEqualTo("user-1");
        assertThat(order.getApprovedBy()).isEqualTo("user-8");
        assertThat(order.getApprovedByName()).isEqualTo("李审批");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("400.00");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("52.00");
        assertThat(order.getFreightAmount()).isEqualByComparingTo("100");
        assertThat(order.getDiscountAmount()).isEqualByComparingTo("50");
        assertThat(order.getPayableAmount()).isEqualByComparingTo("450.00");

        verify(orderMapper).insert(order);

        ArgumentCaptor<PurOrderItem> captor = ArgumentCaptor.forClass(PurOrderItem.class);
        verify(orderItemMapper, times(2)).insert(captor.capture());
        List<PurOrderItem> items = captor.getAllValues();
        assertThat(items.get(0).getOrderId()).isEqualTo(order.getId());
        assertThat(items.get(0).getUnitPrice()).isEqualByComparingTo("150");
        assertThat(items.get(0).getSubtotal()).isEqualByComparingTo("300.00");
        assertThat(items.get(0).getTaxRate()).isEqualByComparingTo("0.13");
        assertThat(items.get(1).getUnitPrice()).isEqualByComparingTo("100");
        assertThat(items.get(1).getSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void convertRefusesARequestThatIsNotApproved() {
        PurRequest pending = approvedRequest().setStatus(1);
        when(requestMapper.selectById("req-1")).thenReturn(pending);

        assertThatThrownBy(() -> service.convert("req-1", convertPayload(), "user-1", "张三"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已审批");

        verify(orderMapper, never()).insert(any(PurOrder.class));
        verify(requestMapper, never()).update(any(), any());
    }

    @Test
    void convertRefusesAConcurrentSecondConversion() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest());
        when(requestItemMapper.selectList(any())).thenReturn(List.of(line("item-1", "螺丝", "2", "100")));
        when(requestMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.convert("req-1", convertPayload(), "user-1", "张三"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("已被转单");

        verify(orderMapper, never()).insert(any(PurOrder.class));
    }

    @Test
    void convertRequiresAPriceForEveryLine() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest());
        when(requestItemMapper.selectList(any())).thenReturn(List.of(line("item-2", "垫片", "1", null)));
        when(requestMapper.update(isNull(), any())).thenReturn(1);

        assertThatThrownBy(() -> service.convert("req-1", convertPayload(), "user-1", "张三"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("缺少单价");

        verify(orderMapper, never()).insert(any(PurOrder.class));
    }

    @Test
    void submitRefusesAnEmptyRequest() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest().setStatus(0));
        when(requestItemMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.submit("req-1"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("至少一条");

        verify(requestMapper, never()).update(any(), any());
    }

    @Test
    void createDraftGeneratesIdNumberAndRequester() {
        PurRequest draft = new PurRequest()
                .setDeptId("dept-1")
                .setExpectedDelivery(LocalDateTime.of(2026, 11, 1, 12, 0))
                .setPurpose("补货");
        PurRequestItem item = new PurRequestItem()
                .setMaterialName("螺丝")
                .setQuantity(new BigDecimal("3"))
                .setUnit("件")
                .setBudgetPrice(new BigDecimal("2.5"));

        PurRequest created = service.createDraft(draft, List.of(item), "user-1", "张三");

        assertThat(created.getId()).isNotBlank();
        assertThat(created.getRequestNo()).startsWith("PR-");
        assertThat(created.getStatus()).isZero();
        assertThat(created.getConverted()).isFalse();
        assertThat(created.getRequesterId()).isEqualTo("user-1");
        assertThat(created.getRequesterName()).isEqualTo("张三");
        assertThat(created.getRequestType()).isEqualTo(1);
        assertThat(created.getPriority()).isEqualTo(2);
        assertThat(created.getBudgetAmount()).isEqualByComparingTo("7.50");

        verify(requestMapper).insert(created);
        ArgumentCaptor<PurRequestItem> captor = ArgumentCaptor.forClass(PurRequestItem.class);
        verify(requestItemMapper).insert(captor.capture());
        PurRequestItem inserted = captor.getValue();
        assertThat(inserted.getRequestId()).isEqualTo(created.getId());
        assertThat(inserted.getRequestNo()).isEqualTo(created.getRequestNo());
        assertThat(inserted.getId()).isNotBlank();
    }

    @Test
    void createDraftRejectsAMissingDepartment() {
        PurRequest draft = new PurRequest()
                .setExpectedDelivery(LocalDateTime.of(2026, 11, 1, 12, 0))
                .setPurpose("补货");

        assertThatThrownBy(() -> service.createDraft(draft, List.of(), "user-1", "张三"))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("申请部门");

        verify(requestMapper, never()).insert(any(PurRequest.class));
    }

    @Test
    void approveRecordsTheCurrentApprover() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest().setStatus(1));
        when(requestMapper.update(isNull(), any())).thenReturn(1);

        assertThat(service.approve("req-1", "user-8", "李审批")).isTrue();

        verify(requestMapper).update(isNull(), any());
    }

    @Test
    void rejectRequiresAReason() {
        when(requestMapper.selectById("req-1")).thenReturn(approvedRequest().setStatus(1));

        assertThatThrownBy(() -> service.reject("req-1", "user-8", "李审批", " "))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("驳回原因");

        verify(requestMapper, never()).update(any(), any());
    }
}
