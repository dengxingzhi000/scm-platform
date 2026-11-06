package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存Mapper
 *
 * <p>数量变更一律走下面的条件更新（单条 UPDATE + WHERE 守卫），
 * 避免“先读后写”在并发下丢失更新，也避免出现负库存：
 * 守卫不满足时返回 0 行，由调用方转成业务失败。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Mapper
public interface InvInventoryMapper extends BaseMapper<Inventory> {

    /**
     * 预占：可用量转锁定，仅当可用量足够时生效。
     *
     * @return 1-成功，0-库存不足或记录已删除
     */
    @Update("UPDATE inv_inventory SET available_stock = available_stock - #{quantity}, "
            + "locked_stock = locked_stock + #{quantity}, version = version + 1, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = false AND available_stock >= #{quantity}")
    int lockStock(@Param("id") String id, @Param("quantity") int quantity);

    /**
     * 确认：锁定库存实扣，总量同步减少。
     *
     * @return 1-成功，0-锁定库存不足
     */
    @Update("UPDATE inv_inventory SET locked_stock = locked_stock - #{quantity}, "
            + "total_stock = total_stock - #{quantity}, version = version + 1, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = false AND locked_stock >= #{quantity} "
            + "AND total_stock >= #{quantity}")
    int confirmStock(@Param("id") String id, @Param("quantity") int quantity);

    /**
     * 释放：锁定库存归还可用量。
     *
     * @return 1-成功，0-锁定库存不足
     */
    @Update("UPDATE inv_inventory SET available_stock = available_stock + #{quantity}, "
            + "locked_stock = locked_stock - #{quantity}, version = version + 1, update_time = NOW() "
            + "WHERE id = #{id} AND deleted = false AND locked_stock >= #{quantity}")
    int releaseStock(@Param("id") String id, @Param("quantity") int quantity);

    /**
     * 调整库存（入库/出库/盘点）：可用量与总量同向变更。
     *
     * <p>扣减时以 {@code available_stock + quantity >= 0} 为守卫，
     * 因此并发扣减不会把可用量扣成负数。
     *
     * @return 1-成功，0-库存不足或记录已删除
     */
    @Update("UPDATE inv_inventory SET available_stock = available_stock + #{quantity}, "
            + "total_stock = total_stock + #{quantity}, version = version + 1, update_time = NOW(), "
            + "update_by = COALESCE(#{operatorId}, update_by), "
            + "last_inbound_at = CASE WHEN #{adjustType} = 1 THEN NOW() ELSE last_inbound_at END, "
            + "last_outbound_at = CASE WHEN #{adjustType} = 2 THEN NOW() ELSE last_outbound_at END "
            + "WHERE id = #{id} AND deleted = false AND available_stock + #{quantity} >= 0")
    int applyAdjustment(@Param("id") String id,
                        @Param("quantity") int quantity,
                        @Param("adjustType") int adjustType,
                        @Param("operatorId") String operatorId);

    /**
     * 入库：数量与移动加权均价在同一条 UPDATE 内原子变更。
     *
     * <p>{@code WHERE version = expectedVersion} 是乐观锁守卫：并发入库时后到的事务
     * 返回 0 行，由调用方重读重试，不会把均价更新覆盖丢失。
     *
     * @return 1-成功，0-版本冲突、记录已删除或库存守卫不满足
     */
    @Update("UPDATE inv_inventory SET available_stock = available_stock + #{quantity}, "
            + "total_stock = total_stock + #{quantity}, average_cost = #{newAverageCost}, "
            + "version = version + 1, update_time = NOW(), "
            + "update_by = COALESCE(#{operatorId}, update_by), "
            + "last_inbound_at = CASE WHEN #{adjustType} = 1 OR #{adjustType} = 7 "
            + "THEN NOW() ELSE last_inbound_at END "
            + "WHERE id = #{id} AND deleted = false AND version = #{expectedVersion} "
            + "AND available_stock + #{quantity} >= 0")
    int applyInboundWithCost(@Param("id") String id,
                             @Param("quantity") int quantity,
                             @Param("newAverageCost") BigDecimal newAverageCost,
                             @Param("expectedVersion") int expectedVersion,
                             @Param("adjustType") int adjustType,
                             @Param("operatorId") String operatorId);

    /**
     * 按 (租户, SKU UUID, 仓库) 精确取台账行。
     *
     * <p>显式 CAST 避免 uuid 列与 varchar 参数比较时的类型错误。
     */
    @Select("SELECT * FROM inv_inventory WHERE tenant_id = CAST(#{tenantId} AS UUID) "
            + "AND sku_id = CAST(#{skuUuid} AS UUID) AND warehouse_id = CAST(#{warehouseId} AS UUID) "
            + "AND deleted = false LIMIT 1")
    Inventory selectBySkuUuidAndWarehouse(@Param("tenantId") String tenantId,
                                          @Param("skuUuid") String skuUuid,
                                          @Param("warehouseId") String warehouseId);

    /**
     * 兼容 scm-order 的数字 SKU：通过 legacy_sku_id 解析台账行。
     *
     * <p>返回列表而非单条，调用方需在结果不唯一（多仓）时拒绝并提示改用 UUID 接口。
     */
    @Select("SELECT * FROM inv_inventory WHERE tenant_id = CAST(#{tenantId} AS UUID) "
            + "AND legacy_sku_id = #{legacySkuId} AND deleted = false")
    List<Inventory> selectByLegacySkuId(@Param("tenantId") String tenantId,
                                        @Param("legacySkuId") Long legacySkuId);
}
