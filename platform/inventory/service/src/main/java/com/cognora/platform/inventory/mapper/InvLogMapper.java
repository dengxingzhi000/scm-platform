package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.InvLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 库存流水 Mapper。
 *
 * <p>写库语句显式 CAST 到 uuid/jsonb，避免 PostgreSQL 对 varchar 参数与
 * uuid/jsonb 列比较/赋值时报类型错误。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Mapper
public interface InvLogMapper extends BaseMapper<InvLog> {

  /**
   * 占用流水幂等键。
   *
   * @return 1-首次占用成功，0-该 (tenant_id, idempotency_key) 已被其它事务写过
   */
  @Insert("INSERT INTO inv_log_idempotency (id, tenant_id, idempotency_key, log_id, create_time) "
          + "VALUES (CAST(#{id} AS UUID), CAST(#{tenantId} AS UUID), #{idempotencyKey}, "
          + "CAST(#{logId} AS UUID), NOW()) "
          + "ON CONFLICT (tenant_id, idempotency_key) DO NOTHING")
  int claimIdempotency(@Param("id") String id,
                       @Param("tenantId") String tenantId,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("logId") String logId);

  @Insert("INSERT INTO inv_log ("
          + "id, tenant_id, sku_id, warehouse_id, legacy_sku_id, warehouse_code, "
          + "batch_no, expiry_date, "
          + "operation_type, business_type, quantity, "
          + "before_stock, after_stock, "
          + "before_total, before_available, before_locked, before_damaged, "
          + "after_total, after_available, after_locked, after_damaged, "
          + "reason, business_key, request_id, reference_type, reference_id, reference_no, "
          + "operator_id, operator_name, ledger_source, extra_data, create_time"
          + ") VALUES ("
          + "CAST(#{log.id} AS UUID), CAST(#{log.tenantId} AS UUID), "
          + "CAST(#{log.skuId} AS UUID), CAST(#{log.warehouseId} AS UUID), "
          + "#{log.legacySkuId}, #{log.warehouseCode}, "
          + "#{log.batchNo}, #{log.expiryDate}, "
          + "#{log.operationType}, #{log.businessType}, #{log.quantity}, "
          + "#{log.beforeStock}, #{log.afterStock}, "
          + "#{log.beforeTotal}, #{log.beforeAvailable}, #{log.beforeLocked}, #{log.beforeDamaged}, "
          + "#{log.afterTotal}, #{log.afterAvailable}, #{log.afterLocked}, #{log.afterDamaged}, "
          + "#{log.reason}, #{log.businessKey}, #{log.requestId}, "
          + "#{log.referenceType}, CAST(#{log.referenceId} AS UUID), #{log.referenceNo}, "
          + "CAST(#{log.operatorId} AS UUID), #{log.operatorName}, "
          + "#{log.ledgerSource}, CAST(#{log.extraData} AS jsonb), #{log.createTime}"
          + ")")
  int insertLog(@Param("log") InvLog log);
}
