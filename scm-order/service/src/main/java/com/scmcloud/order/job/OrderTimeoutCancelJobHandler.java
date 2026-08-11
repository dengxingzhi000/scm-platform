package com.scmcloud.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scmcloud.inventory.api.InventoryDubboService;
import com.scmcloud.order.domain.entity.OrdOrder;
import com.scmcloud.order.domain.entity.OrderStatus;
import com.scmcloud.order.mapper.OrdOrderMapper;
import com.scmcloud.order.service.command.OrdOrderCommandService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消任务
 *
 * <p>定时扫描超时未支付的订单，自动取消并释放库存。
 *
 * <p>执行频率: 每分钟执行一次
 * <p>超时时间: 默认 30 分钟（可通过任务参数配置）
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Component
public class OrderTimeoutCancelJobHandler {

    private static final int DEFAULT_TIMEOUT_MINUTES = 30;
    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final String CANCEL_REASON = "订单超时未支付,系统自动取消";
    private static final String RELEASE_PREFIX = "TIMEOUT_CANCEL:";

    private final OrdOrderMapper orderMapper;
    private final OrdOrderCommandService ordOrderCommandService;

    public OrderTimeoutCancelJobHandler(OrdOrderMapper orderMapper,
                                        OrdOrderCommandService ordOrderCommandService) {
        this.orderMapper = orderMapper;
        this.ordOrderCommandService = ordOrderCommandService;
    }

    @DubboReference(version = "1.0.0", group = "scm", check = false)
    private InventoryDubboService inventoryService;

    /**
     * 订单超时自动取消任务
     *
     * <p>任务参数: 超时分钟数（默认 30）
     *
     * @throws Exception 任务执行异常
     */
    @XxlJob("orderTimeoutCancelJobHandler")
    public void execute() throws Exception {
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("[订单超时取消] 开始执行任务");

        try {
            int timeoutMinutes = resolveTimeoutMinutes();

            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
            List<OrdOrder> timeoutOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<OrdOrder>()
                            .eq(OrdOrder::getStatus, OrderStatus.PENDING_PAYMENT.getCode())
                            .lt(OrdOrder::getCreateTime, timeoutThreshold)
                            .orderByAsc(OrdOrder::getCreateTime)
                            .last("LIMIT " + DEFAULT_BATCH_SIZE)
            );

            if (timeoutOrders.isEmpty()) {
                XxlJobHelper.log("[订单超时取消] 无超时订单，任务结束");
                return;
            }

            XxlJobHelper.log("[订单超时取消] 发现超时订单: count={}, timeoutMinutes={}",
                    timeoutOrders.size(), timeoutMinutes);

            int successCount = 0;
            int failCount = 0;

            for (OrdOrder order : timeoutOrders) {
                try {
                    ordOrderCommandService.cancelTimeoutOrder(order);
                    releaseStock(order);
                    successCount++;
                    XxlJobHelper.log("取消成功: orderNo={}, createTime={}",
                            order.getOrderNo(), order.getCreateTime());
                } catch (Exception e) {
                    failCount++;
                    XxlJobHelper.log("取消失败: orderNo={}, error={}",
                            order.getOrderNo(), e.getMessage());
                    log.error("订单取消失败: orderNo={}", order.getOrderNo(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("[订单超时取消] 任务完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    timeoutOrders.size(), successCount, failCount, duration);

            if (failCount > 0) {
                XxlJobHelper.handleFail(String.format("部分订单取消失败: 总数=%d, 成功=%d, 失败=%d",
                        timeoutOrders.size(), successCount, failCount));
            } else {
                XxlJobHelper.handleSuccess(String.format("所有订单取消成功 总数=%d, 耗时=%dms",
                        successCount, duration));
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("[订单超时取消] 任务异常: error={}, 耗时={}ms",
                    e.getMessage(), duration);
            log.error("订单超时取消任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
            throw e;
        }
    }

    private int resolveTimeoutMinutes() {
        String param = XxlJobHelper.getJobParam();
        if (param == null || param.trim().isEmpty()) {
            return DEFAULT_TIMEOUT_MINUTES;
        }
        try {
            return Integer.parseInt(param.trim());
        } catch (NumberFormatException e) {
            XxlJobHelper.log("[订单超时取消] 参数格式错误，使用默认 {} 分钟: param={}",
                    DEFAULT_TIMEOUT_MINUTES, param);
            return DEFAULT_TIMEOUT_MINUTES;
        }
    }

    private void releaseStock(OrdOrder order) {
        if (order.getQuantity() == null) {
            throw new IllegalArgumentException("订单数量为空，无法释放库存: orderNo=" + order.getOrderNo());
        }

        long skuId;
        try {
            skuId = Long.parseLong(order.getSkuId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("skuId 非法，无法释放库存: orderNo=" + order.getOrderNo()
                    + ", skuId=" + order.getSkuId(), e);
        }

        inventoryService.releaseStock(skuId, order.getQuantity().getValue(), RELEASE_PREFIX + order.getOrderNo());
        log.info("订单超时自动取消成功: orderNo={}, skuId={}, quantity={}",
                order.getOrderNo(), order.getSkuId(), order.getQuantity().getValue());
    }
}