package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.InvCostLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存成本流水 Mapper。
 *
 * <p>写入显式 CAST 到 uuid/jsonb，避免 PostgreSQL 对 varchar 参数做严格类型比较时报错。
 * {@code ON CONFLICT (tenant_id, idempotency_key) DO NOTHING} 保证重复回调/重复
 * requestId 不会重复记账。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Mapper
public interface InvCostLogMapper extends BaseMapper<InvCostLog> {

  @Insert("INSERT INTO inv_cost_log ("
          + "id, tenant_id, inventory_id, sku_id, warehouse_id, legacy_sku_id, warehouse_code, "
          + "direction, quantity, unit_price, amount, "
          + "before_average_cost, after_average_cost, before_balance_qty, after_balance_qty, "
          + "business_type, business_key, request_id, reference_type, reference_id, reference_no, "
          + "counterpart_key, operator_id, operator_name, ledger_source, idempotency_key, "
          + "remark, extra_data, create_time"
          + ") VALUES ("
          + "CAST(#{log.id} AS UUID), CAST(#{log.tenantId} AS UUID), "
          + "CAST(#{log.inventoryId} AS UUID), CAST(#{log.skuId} AS UUID), "
          + "CAST(#{log.warehouseId} AS UUID), #{log.legacySkuId}, #{log.warehouseCode}, "
          + "#{log.direction}, #{log.quantity}, #{log.unitPrice}, #{log.amount}, "
          + "#{log.beforeAverageCost}, #{log.afterAverageCost}, "
          + "#{log.beforeBalanceQty}, #{log.afterBalanceQty}, "
          + "#{log.businessType}, #{log.businessKey}, #{log.requestId}, "
          + "#{log.referenceType}, CAST(#{log.referenceId} AS UUID), #{log.referenceNo}, "
          + "#{log.counterpartKey}, CAST(#{log.operatorId} AS UUID), #{log.operatorName}, "
          + "#{log.ledgerSource}, #{log.idempotencyKey}, #{log.remark}, "
          + "CAST(#{log.extraData} AS jsonb), #{log.createTime}"
          + ") ON CONFLICT (tenant_id, idempotency_key) DO NOTHING")
  int insertLog(@Param("log") InvCostLog log);
}
