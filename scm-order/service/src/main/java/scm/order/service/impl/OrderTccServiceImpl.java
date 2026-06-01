package scm.order.service.impl;

import com.frog.inventory.api.InventoryTccService;
import com.frog.order.api.OrderDubboService;
import com.frog.order.api.dto.OrderVO;
import com.frog.order.api.request.CreateOrderRequest;
import scm.order.domain.entity.OrdOrder;
import scm.order.mapper.OrdOrderMapper;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 订单 TCC 服务实现
 *
 * <p>使用 TCC 模式创建订单，与库存预留配合实现分布式事务
 *
 * <p>对比 AT 模式：
 * <ul>
 *   <li>AT 模式：自动回滚，基于 undo_log，业务无侵入</li>
 *   <li>TCC 模式：手动补偿，业务需实现 Try/Confirm/Cancel，控制更灵活</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class OrderTccServiceImpl {

    private final OrdOrderMapper orderMapper;

    @DubboReference(version = "1.0.0", group = "scm", check = false)
    private InventoryTccService inventoryTccService;

    /**
     * 使用 TCC 模式创建订单
     *
     * <p>流程：
     * 1. 开启全局事务
     * 2. 创建订单记录
     * 3. 调用库存 TCC 服务预留库存（Try 阶段）
     * 4. 全局事务提交时，Seata 自动调用 Confirm
     * 5. 全局事务回滚时，Seata 自动调用 Cancel
     *
     * @param request 创建订单请求
     * @return 订单 VO
     */
    @GlobalTransactional(
            name = "create-order-tcc",
            rollbackFor = Exception.class,
            timeoutMills = 30000
    )
    public OrderVO createOrderWithTcc(CreateOrderRequest request) {
        String xid = RootContext.getXID();
        log.info("🌐 [订单-TCC] 开始创建订单: UserId={}, SkuId={}, Qty={}, XID={}",
                request.getUserId(), request.getSkuId(), request.getQuantity(), xid);

        long startTime = System.currentTimeMillis();

        try {
            // 1. 创建订单记录（本地事务）
            OrdOrder order = new OrdOrder();
            order.setOrderNo(generateOrderNo());
            order.setUserId(String.valueOf(request.getUserId()));
            order.setSkuId(String.valueOf(request.getSkuId()));
            order.setQuantity(request.getQuantity() != null ? request.getQuantity().intValue() : null);
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus(0); // PENDING_PAYMENT
            order.setRemark("[TCC模式] " + request.getRemark());
            order.setCreateTime(LocalDateTime.now());

            orderMapper.insert(order);
            log.info("✅ [订单-TCC] 订单创建成功: OrderNo={}, XID={}", order.getOrderNo(), xid);

            // 2. 调用库存 TCC 服务预留库存（Try 阶段）
            try {
                boolean reserved = inventoryTccService.reserveInventory(
                        request.getSkuId(),
                        request.getQuantity(),
                        order.getOrderNo()  // 订单号作为业务键
                );

                if (!reserved) {
                    log.error("❌ [订单-TCC] 库存预留失败: OrderNo={}, XID={}", order.getOrderNo(), xid);
                    throw new RuntimeException("库存预留失败");
                }

                log.info("✅ [订单-TCC] 库存预留成功: OrderNo={}, SKU={}, Qty={}, XID={}",
                        order.getOrderNo(), request.getSkuId(), request.getQuantity(), xid);
            } catch (Exception e) {
                log.error("❌ [订单-TCC] 库存预留异常: OrderNo={}, XID={}, 原因={}",
                        order.getOrderNo(), xid, e.getMessage());
                throw new RuntimeException("库存预留失败: " + e.getMessage());
            }

            // 3. 转换为 VO 返回
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);

            long duration = System.currentTimeMillis() - startTime;
            log.info("🎉 [订单-TCC] 订单创建完成: OrderNo={}, XID={}, 耗时={}ms",
                    order.getOrderNo(), xid, duration);

            return vo;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("💥 [订单-TCC] 订单创建失败，全局事务回滚: XID={}, 耗时={}ms, 原因={}",
                    xid, duration, e.getMessage());
            throw e;
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = new Random().nextInt(10000);
        return String.format("TCC%s%04d", timestamp, random);
    }
}