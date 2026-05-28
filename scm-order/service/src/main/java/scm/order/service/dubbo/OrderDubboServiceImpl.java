package scm.order.service.dubbo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.frog.order.api.OrderDubboService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import scm.order.domain.entity.OrdOrder;
import scm.order.service.IOrdOrderService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单Dubbo服务实现
 *
 * <p>提供订单创建、查询、取消等RPC接口，供其他微服务调用
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@DubboService
@Component
@RequiredArgsConstructor
public class OrderDubboServiceImpl implements OrderDubboService {

    private final IOrdOrderService orderService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("创建订单请求不能为空");
        }

        log.info("Dubbo创建订单: userId={}, skuId={}, quantity={}",
                request.getUserId(), request.getSkuId(), request.getQuantity());

        // 构建订单实体
        OrdOrder order = new OrdOrder();
        order.setId(UUID.randomUUID().toString().replace("-", ""));
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId() != null ? request.getUserId().toString() : null);
        order.setOrderType(1); // 普通订单
        order.setOrderSource("API");
        order.setStatus(0); // 待支付
        order.setTotalAmount(request.getTotalAmount());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFreightAmount(BigDecimal.ZERO);
        order.setPayableAmount(request.getTotalAmount());
        order.setBuyerMessage(request.getRemark());
        order.setDeleted(false);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        // 保存订单
        boolean success = orderService.save(order);
        if (!success) {
            throw new RuntimeException("创建订单失败");
        }

        log.info("Dubbo创建订单成功: orderNo={}, id={}", order.getOrderNo(), order.getId());

        return convertToVO(order);
    }

    @Override
    public OrderVO queryOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.debug("Dubbo查询订单: orderNo={}", orderNo);

        LambdaQueryWrapper<OrdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrdOrder::getOrderNo, orderNo)
                .eq(OrdOrder::getDeleted, false);

        OrdOrder order = orderService.getOne(wrapper);
        if (order == null) {
            log.warn("Dubbo查询订单不存在: orderNo={}", orderNo);
            return null;
        }

        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        log.info("Dubbo取消订单: orderNo={}", orderNo);

        LambdaQueryWrapper<OrdOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrdOrder::getOrderNo, orderNo)
                .eq(OrdOrder::getDeleted, false);

        OrdOrder order = orderService.getOne(wrapper);
        if (order == null) {
            throw new RuntimeException("订单不存在: " + orderNo);
        }

        // 只有待支付状态可以取消
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不允许取消: orderNo=" + orderNo + ", status=" + order.getStatus());
        }

        order.setStatus(7); // 已取消
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason("Dubbo接口取消");
        order.setUpdateTime(LocalDateTime.now());

        boolean success = orderService.updateById(order);
        if (!success) {
            throw new RuntimeException("取消订单失败: " + orderNo);
        }

        log.info("Dubbo取消订单成功: orderNo={}", orderNo);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    /**
     * 转换为VO对象
     */
    private OrderVO convertToVO(OrdOrder order) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId() != null ? Long.parseLong(order.getId()) : null);
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId() != null ? Long.parseLong(order.getUserId()) : null);
        vo.setStatus(order.getStatus() != null ? String.valueOf(order.getStatus()) : null);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setRemark(order.getBuyerMessage());
        vo.setCreateTime(order.getCreateTime());
        return vo;
    }
}
