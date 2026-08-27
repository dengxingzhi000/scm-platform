package com.scmcloud.order.dto;

import com.scmcloud.common.domain.Money;
import com.scmcloud.common.domain.Quantity;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrdOrderItem;
import com.scmcloud.order.domain.entity.OrdPayment;
import com.scmcloud.order.domain.entity.OrdRefund;
import com.scmcloud.order.domain.entity.OrdRefundItem;
import com.scmcloud.order.domain.entity.OrdStatusHistory;
import com.scmcloud.order.domain.entity.PaymentStatus;
import com.scmcloud.order.domain.entity.RefundStatus;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * OrdOrder 实体 ↔ OrderResponse DTO 转换器。
 *
 * <p>仅做字段映射，不做业务逻辑。控制层负责把领域对象转成对外 DTO，
 * 屏蔽内部审计字段（createBy/updateBy/deleted/extra_data）和领域 transient 字段（domainEvents）。</p>
 *
 * @author SCM Platform Team
 */
public final class OrderConverter {

    private OrderConverter() {
    }

    /**
     * 实体 → DTO（不含明细）。
     */
    public static OrderResponse toResponse(OrdOrder order) {
        if (order == null) {
            return null;
        }
        OrderResponse r = new OrderResponse();
        BeanUtils.copyProperties(order, r, "items", "domainEvents");
        // Money / Quantity 转 BigDecimal / Integer
        r.setTotalAmount(moneyToBigDecimal(order.getTotalAmount()));
        r.setDiscountAmount(moneyToBigDecimal(order.getDiscountAmount()));
        r.setFreightAmount(moneyToBigDecimal(order.getFreightAmount()));
        r.setPayableAmount(moneyToBigDecimal(order.getPayableAmount()));
        r.setPaidAmount(moneyToBigDecimal(order.getPaidAmount()));
        if (order.getQuantity() != null) {
            r.setQuantity(order.getQuantity().getValue());
        }
        // 状态码转枚举（含本地化描述）
        if (order.getStatus() != null) {
            r.setStatus(OrderStatus.fromCode(order.getStatus()));
            r.setStatusName(r.getStatus().getDescription());
        }
        return r;
    }

    /**
     * 实体列表 → DTO 列表。
     */
    public static List<OrderResponse> toResponseList(List<OrdOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream().map(OrderConverter::toResponse).collect(Collectors.toList());
    }

    /**
     * 明细实体 → DTO。
     */
    public static OrderItemResponse toItemResponse(OrdOrderItem item) {
        if (item == null) {
            return null;
        }
        OrderItemResponse r = new OrderItemResponse();
        BeanUtils.copyProperties(item, r);
        r.setOriginalPrice(moneyToBigDecimal(item.getOriginalPrice()));
        r.setSellingPrice(moneyToBigDecimal(item.getSellingPrice()));
        r.setSubtotal(moneyToBigDecimal(item.getSubtotal()));
        r.setDiscountAmount(moneyToBigDecimal(item.getDiscountAmount()));
        r.setFinalAmount(moneyToBigDecimal(item.getFinalAmount()));
        r.setRefundAmount(moneyToBigDecimal(item.getRefundAmount()));
        if (item.getQuantity() != null) {
            r.setQuantity(item.getQuantity().getValue());
        }
        if (item.getRefundQuantity() != null) {
            r.setRefundQuantity(item.getRefundQuantity().getValue());
        }
        return r;
    }

    private static BigDecimal moneyToBigDecimal(Money money) {
        return money == null ? null : money.getAmount();
    }

    /**
     * 明细请求 DTO → 实体。
     */
    public static OrdOrderItem toItemEntity(OrderItemRequest req) {
        if (req == null) {
            return null;
        }
        OrdOrderItem item = new OrdOrderItem();
        if (req.getSkuId() != null && !req.getSkuId().isBlank()) {
            item.setSkuId(UUID.fromString(req.getSkuId()));
        }
        if (req.getSpuId() != null && !req.getSpuId().isBlank()) {
            item.setSpuId(UUID.fromString(req.getSpuId()));
        }
        item.setSkuCode(req.getSkuCode());
        item.setSkuName(req.getSkuName());
        item.setSpuName(req.getSpuName());
        if (req.getSellingPrice() != null) {
            item.setSellingPrice(Money.of(req.getSellingPrice()));
        }
        if (req.getQuantity() != null) {
            item.setQuantity(Quantity.of(req.getQuantity()));
        }
        item.setWarehouseId(req.getWarehouseId());
        item.setRemark(req.getRemark());
        return item;
    }

    /**
     * 创建请求 → 订单头 + 明细实体对。
     *
     * <p>仅做字段映射；{@code totalAmount} / {@code payableAmount} 等业务字段
     * 留给 {@code OrdOrderCommandService.createOrder()} 在事务内计算并回填。</p>
     */
    public static OrderDraft toOrderEntity(OrderCreateRequest req) {
        if (req == null) {
            return null;
        }
        OrdOrder order = new OrdOrder();
        order.setOrderNo(req.getOrderNo());
        order.setUserId(req.getUserId());
        order.setUsername(req.getUsername());
        order.setOrderType(req.getOrderType());
        order.setOrderSource(req.getOrderSource());
        order.setShippingAddress(req.getShippingAddress());
        order.setPaymentMethod(req.getPaymentMethod());
        if (req.getDiscountAmount() != null) {
            order.setDiscountAmount(Money.of(req.getDiscountAmount()));
        }
        if (req.getFreightAmount() != null) {
            order.setFreightAmount(Money.of(req.getFreightAmount()));
        }
        order.setBuyerMessage(req.getBuyerMessage());

        List<OrdOrderItem> items = req.getItems().stream()
                .map(OrderConverter::toItemEntity)
                .collect(Collectors.toList());
        return new OrderDraft(order, items);
    }

    /**
     * 订单创建草稿：订单头 + 明细实体对。
     */
    public record OrderDraft(OrdOrder order, List<OrdOrderItem> items) {
    }

    // ─── 支付 ────────────────────────────────────────────────────

    /**
     * 支付实体 → DTO。
     */
    public static PaymentResponse toPaymentResponse(OrdPayment payment) {
        if (payment == null) {
            return null;
        }
        PaymentResponse r = new PaymentResponse();
        BeanUtils.copyProperties(payment, r);
        r.setPaymentAmount(moneyToBigDecimal(payment.getPaymentAmount()));
        r.setRefundAmount(moneyToBigDecimal(payment.getRefundAmount()));
        if (payment.getStatusEnum() != null) {
            r.setPaymentStatus(payment.getStatusEnum().name());
        }
        return r;
    }

    public static List<PaymentResponse> toPaymentResponseList(List<OrdPayment> payments) {
        if (payments == null || payments.isEmpty()) {
            return Collections.emptyList();
        }
        return payments.stream().map(OrderConverter::toPaymentResponse).collect(Collectors.toList());
    }

    /**
     * 支付创建请求 → 实体。
     */
    public static OrdPayment toPaymentEntity(PaymentCreateRequest req) {
        if (req == null) {
            return null;
        }
        OrdPayment p = new OrdPayment();
        p.setPaymentNo(req.getPaymentNo());
        p.setOrderId(req.getOrderId());
        p.setOrderNo(req.getOrderNo());
        p.setUserId(req.getUserId());
        p.setPaymentMethod(req.getPaymentMethod());
        p.setPaymentChannel(req.getPaymentChannel());
        if (req.getPaymentAmount() != null) {
            p.setPaymentAmount(Money.of(req.getPaymentAmount()));
        }
        p.setRemark(req.getRemark());
        return p;
    }

    // ─── 退款 ────────────────────────────────────────────────────

    /**
     * 退款实体 → DTO。
     */
    public static RefundResponse toRefundResponse(OrdRefund refund) {
        if (refund == null) {
            return null;
        }
        RefundResponse r = new RefundResponse();
        BeanUtils.copyProperties(refund, r, "items");
        r.setRefundAmount(moneyToBigDecimal(refund.getRefundAmount()));
        if (refund.getStatusEnum() != null) {
            r.setRefundStatus(refund.getStatusEnum().name());
        }
        return r;
    }

    public static List<RefundResponse> toRefundResponseList(List<OrdRefund> refunds) {
        if (refunds == null || refunds.isEmpty()) {
            return Collections.emptyList();
        }
        return refunds.stream().map(OrderConverter::toRefundResponse).collect(Collectors.toList());
    }

    /**
     * 退款明细实体 → DTO。
     */
    public static RefundItemResponse toRefundItemResponse(OrdRefundItem item) {
        if (item == null) {
            return null;
        }
        RefundItemResponse r = new RefundItemResponse();
        BeanUtils.copyProperties(item, r);
        r.setRefundAmount(moneyToBigDecimal(item.getRefundAmount()));
        return r;
    }

    public static List<RefundItemResponse> toRefundItemResponseList(List<OrdRefundItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(OrderConverter::toRefundItemResponse).collect(Collectors.toList());
    }

    /**
     * 退款创建请求 → 退款实体（不含 items，由 caller 单独处理）。
     */
    public static OrdRefund toRefundEntity(RefundCreateRequest req) {
        if (req == null) {
            return null;
        }
        OrdRefund refund = new OrdRefund();
        refund.setRefundNo(req.getRefundNo());
        refund.setOrderId(req.getOrderId());
        refund.setOrderNo(req.getOrderNo());
        refund.setUserId(req.getUserId());
        refund.setRefundType(req.getRefundType());
        refund.setReason(req.getReason());
        refund.setDescription(req.getDescription());
        if (req.getRefundAmount() != null) {
            refund.setRefundAmount(Money.of(req.getRefundAmount()));
        }
        refund.setRemark(null);
        return refund;
    }

    /**
     * 退款明细请求 → 实体。
     */
    public static OrdRefundItem toRefundItemEntity(RefundItemRequest req) {
        if (req == null) {
            return null;
        }
        OrdRefundItem item = new OrdRefundItem();
        if (req.getOrderItemId() != null) {
            item.setOrderItemId(req.getOrderItemId());
        }
        if (req.getSkuId() != null && !req.getSkuId().isBlank()) {
            item.setSkuId(UUID.fromString(req.getSkuId()));
        }
        item.setSkuCode(req.getSkuCode());
        item.setSkuName(req.getSkuName());
        if (req.getQuantity() != null) {
            item.setQuantity(req.getQuantity());
        }
        if (req.getRefundAmount() != null) {
            item.setRefundAmount(Money.of(req.getRefundAmount()));
        }
        item.setRemark(req.getRemark());
        return item;
    }

    // ─── 状态历史 ────────────────────────────────────────────────

    /**
     * 状态历史实体 → DTO。
     */
    public static StatusHistoryResponse toStatusHistoryResponse(OrdStatusHistory h) {
        if (h == null) {
            return null;
        }
        StatusHistoryResponse r = new StatusHistoryResponse();
        BeanUtils.copyProperties(h, r);
        if (h.getFromStatus() != null) {
            r.setFromStatus(h.getFromStatus());
            OrderStatus fromEnum = OrderStatus.fromCode(h.getFromStatus());
            r.setFromStatusName(fromEnum.getDescription());
        }
        if (h.getToStatus() != null) {
            OrderStatus toEnum = OrderStatus.fromCode(h.getToStatus());
            r.setToStatus(toEnum);
            r.setToStatusName(toEnum.getDescription());
        }
        return r;
    }

    public static List<StatusHistoryResponse> toStatusHistoryResponseList(List<OrdStatusHistory> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        return history.stream().map(OrderConverter::toStatusHistoryResponse).collect(Collectors.toList());
    }
}
