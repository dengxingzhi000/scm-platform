package com.frog.inventory.api;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 库存 TCC 服务接口
 *
 * <p>TCC (Try-Confirm-Cancel) 模式实现库存预留
 *
 * <p>三阶段说明：
 * <ul>
 *   <li>Try: 预留库存（将可用库存转为锁定库存）</li>
 *   <li>Confirm: 确认扣减（扣减锁定库存）</li>
 *   <li>Cancel: 取消预留（释放锁定库存为可用库存）</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@LocalTCC
public interface InventoryTccService {

    /**
     * Try 阶段：预留库存
     *
     * <p>将可用库存转为锁定库存，但不实际扣减
     *
     * @param skuId SKU ID
     * @param quantity 预留数量
     * @param businessKey 业务键（订单号），用于幂等性控制
     * @return 预留是否成功
     */
    @TwoPhaseBusinessAction(
            name = "reserveInventory",
            commitMethod = "confirmReserve",
            rollbackMethod = "cancelReserve"
    )
    boolean reserveInventory(
            @BusinessActionContextParameter(paramName = "skuId") Long skuId,
            @BusinessActionContextParameter(paramName = "quantity") Integer quantity,
            @BusinessActionContextParameter(paramName = "businessKey") String businessKey
    );

    /**
     * Confirm 阶段：确认预留
     *
     * <p>扣减锁定库存，完成最终扣减
     *
     * @param context TCC 上下文
     * @return 确认是否成功
     */
    boolean confirmReserve(BusinessActionContext context);

    /**
     * Cancel 阶段：取消预留
     *
     * <p>释放锁定库存，恢复为可用库存
     *
     * @param context TCC 上下文
     * @return 取消是否成功
     */
    boolean cancelReserve(BusinessActionContext context);
}