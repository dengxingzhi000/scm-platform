package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.InvBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 库存批次 Mapper。
 *
 * <p>扣减使用条件 UPDATE（{@code available_quantity >= quantity}）作为并发守卫：
 * 并发事务同时读到同一批次的可用量时，只有一个能扣减成功，另一个返回 0，
 * 由上层整单失败而不是重复分配。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Mapper
public interface InvBatchMapper extends BaseMapper<InvBatch> {

  /**
   * 该 SKU/仓库是否存在批次记录（含已耗尽/过期批次），用于区分
   * “批次管理商品”与“无批次台账”的回退口径。
   */
  @Select("SELECT COUNT(*) FROM inv_batch "
      + "WHERE tenant_id = CAST(#{tenantId} AS UUID) "
      + "AND sku_id = CAST(#{skuId} AS UUID) "
      + "AND warehouse_id = CAST(#{warehouseId} AS UUID) "
      + "AND deleted = FALSE")
  int countManagedBatches(@Param("tenantId") String tenantId,
                          @Param("skuId") String skuId,
                          @Param("warehouseId") String warehouseId);

  /**
   * FEFO 候选批次：合格、有可用量、未删除，按效期升序（空效期排最后），
   * 同效期按入库时间升序。status=3（已过期）也返回，由分配器显式跳过并告警，
   * 避免过期批次被安静地忽略。
   */
  @Select("SELECT id, tenant_id, batch_code, sku_id, warehouse_id, "
      + "production_date, expiry_date, shelf_life_days, quality_status, "
      + "total_quantity, available_quantity, locked_quantity, damaged_quantity, "
      + "unit_cost, status, create_time, update_time, deleted "
      + "FROM inv_batch "
      + "WHERE tenant_id = CAST(#{tenantId} AS UUID) "
      + "AND sku_id = CAST(#{skuId} AS UUID) "
      + "AND warehouse_id = CAST(#{warehouseId} AS UUID) "
      + "AND deleted = FALSE "
      + "AND quality_status = 1 "
      + "AND available_quantity > 0 "
      + "AND status IN (1, 2, 3) "
      + "ORDER BY expiry_date ASC NULLS LAST, create_time ASC, id ASC")
  List<InvBatch> selectFefoCandidates(@Param("tenantId") String tenantId,
                                      @Param("skuId") String skuId,
                                      @Param("warehouseId") String warehouseId);

  /**
   * 条件扣减批次可用量。
   *
   * @return 1-扣减成功；0-可用量已被并发事务改小或批次已删除
   */
  @Update("UPDATE inv_batch SET "
      + "available_quantity = available_quantity - #{quantity}, "
      + "total_quantity = total_quantity - #{quantity}, "
      + "status = CASE WHEN available_quantity - #{quantity} <= 0 THEN 0 ELSE status END, "
      + "update_time = NOW() "
      + "WHERE id = CAST(#{batchId} AS UUID) "
      + "AND tenant_id = CAST(#{tenantId} AS UUID) "
      + "AND deleted = FALSE "
      + "AND available_quantity >= #{quantity}")
  int deductAvailable(@Param("batchId") String batchId,
                      @Param("tenantId") String tenantId,
                      @Param("quantity") int quantity);
}
