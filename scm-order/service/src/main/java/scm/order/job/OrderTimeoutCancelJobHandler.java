package scm.order.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.frog.inventory.api.InventoryDubboService;
import com.frog.order.domain.entity.Order;
import com.frog.order.mapper.OrdOrderMapper;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消任务
 *
 * <p>定时扫描超时未支付的订单，自动取消并释放库存
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

    @Autowired
    private OrdOrderMapper orderMapper;

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
        XxlJobHelper.log("⏰ [订单超时取消] 开始执行任务");

        try {
            // 1. 获取任务参数（超时分钟数，默认 30）
            String param = XxlJobHelper.getJobParam();
            int timeoutMinutes = 30;
            if (param != null && !param.trim().isEmpty()) {
                try {
                    timeoutMinutes = Integer.parseInt(param.trim());
                } catch (NumberFormatException e) {
                    XxlJobHelper.log("⚠️  [订单超时取消] 参数格式错误，使用默认值 30 分钟: param={}", param);
                }
            }

            // 2. 查询超时订单
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
            List<Order> timeoutOrders = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>()
                            .eq(Order::getStatus, "PENDING_PAYMENT")
                            .lt(Order::getCreateTime, timeoutThreshold)
                            .last("LIMIT 1000")  // 每次最多处理 1000 条
            );

            if (timeoutOrders.isEmpty()) {
                XxlJobHelper.log("✅ [订单超时取消] 无超时订单，任务结束");
                return;
            }

            XxlJobHelper.log("📋 [订单超时取消] 发现超时订单: count={}, timeoutMinutes={}",
                    timeoutOrders.size(), timeoutMinutes);

            // 3. 批量取消订单
            int successCount = 0;
            int failCount = 0;

            for (Order order : timeoutOrders) {
                try {
                    cancelOrder(order);
                    successCount++;
                    XxlJobHelper.log("  ✓ 取消成功: orderNo={}, createTime={}",
                            order.getOrderNo(), order.getCreateTime());
                } catch (Exception e) {
                    failCount++;
                    XxlJobHelper.log("  ✗ 取消失败: orderNo={}, error={}",
                            order.getOrderNo(), e.getMessage());
                    log.error("订单取消失败", e);
                }
            }

            // 4. 统计结果
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("🎉 [订单超时取消] 任务完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    timeoutOrders.size(), successCount, failCount, duration);

            // 5. 设置任务结果
            if (failCount > 0) {
                XxlJobHelper.handleFail(String.format("部分订单取消失败: 总数=%d, 成功=%d, 失败=%d",
                        timeoutOrders.size(), successCount, failCount));
            } else {
                XxlJobHelper.handleSuccess(String.format("所有订单取消成功: 总数=%d, 耗时=%dms",
                        successCount, duration));
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            XxlJobHelper.log("❌ [订单超时取消] 任务异常: error={}, 耗时={}ms",
                    e.getMessage(), duration);
            log.error("订单超时取消任务执行失败", e);
            XxlJobHelper.handleFail("任务执行失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 取消单个订单（分布式事务）
     *
     * @param order 订单
     */
    @GlobalTransactional(name = "cancel-timeout-order", rollbackFor = Exception.class)
    public void cancelOrder(Order order) {
        // 1. 更新订单状态为已取消
        int updated = orderMapper.update(null,
                new LambdaUpdateWrapper<Order>()
                        .set(Order::getStatus, "CANCELLED_TIMEOUT")
                        .eq(Order::getId, order.getId())
                        .eq(Order::getStatus, "PENDING_PAYMENT")  // 乐观锁
        );

        if (updated == 0) {
            throw new RuntimeException("订单状态已变更，无法取消");
        }

        // 2. 释放库存（RPC 调用，参与分布式事务）
        inventoryService.releaseStock(
                order.getSkuId(),
                order.getQuantity(),
                "TIMEOUT_CANCEL:" + order.getOrderNo()
        );

        log.info("订单超时自动取消成功: orderNo={}, skuId={}, quantity={}",
                order.getOrderNo(), order.getSkuId(), order.getQuantity());
    }
}