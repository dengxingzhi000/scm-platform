package com.cognora.platform.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.common.tenant.TenantCacheKeys;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.ReservationStatus;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryReservationRequest;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import com.cognora.platform.inventory.service.IInvReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 库存预占服务实现（DB 持久化状态机 + 条件更新）。
 *
 * <p>与旧实现的差异：
 * <ul>
 *   <li>预占记录落在 {@code inv_reservation_record}，Redis 不再是唯一事实来源；
 *       Redis TTL 到期不会再把锁定库存“弄丢”。</li>
 *   <li>幂等键为 (tenant_id, business_key)，重复预占不会二次扣减。</li>
 *   <li>确认/释放/过期都通过带 {@code status = 'RESERVED'} 守卫的单条 UPDATE
 *       流转状态，重复调用幂等，非法流转（如释放已确认的预占）被拒绝。</li>
 *   <li>库存数量变更使用 {@code available_stock >= ?} / {@code locked_stock >= ?}
 *       守卫的条件更新，并发下不会出现负库存。</li>
 *   <li>过期预占由 {@link #releaseExpiredReservations()} 真正释放库存。</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class InvReservationServiceImpl implements IInvReservationService {

  private final InvInventoryMapper inventoryMapper;

  private final InvReservationRecordMapper reservationRecordMapper;

  private final DistributedLock distributedLock;

  private static final int DEFAULT_TIMEOUT_SECONDS = 900; // 15分钟

  private static final int EXPIRY_BATCH_SIZE = 200;

  @Override
  @Master(reason = "写操作必须走主库")
  @Transactional(rollbackFor = Exception.class)
  public boolean reserveInventory(InventoryReservationRequest request) {
    UUID tenantId = TenantContextHolder.getRequiredTenantId();
    String tenant = tenantId.toString();

    if (request.getQuantity() == null || request.getQuantity() <= 0) {
      throw new IllegalArgumentException("预占数量必须大于0");
    }
    int quantity = request.getQuantity();

    log.debug("预占库存: skuId={}, warehouseId={}, quantity={}, businessKey={}",
        request.getSkuId(), request.getWarehouseId(), quantity, request.getBusinessKey());

    // 同一业务键串行化：幂等检查与写库都在这把锁内完成，避免并发重复预占。
    String lockKey = TenantCacheKeys.scoped("reserve", request.getBusinessKey());
    DistributedLock.LockHandle lock = distributedLock.tryLock(lockKey, 10, TimeUnit.SECONDS);
    if (lock == null) {
      throw new IllegalStateException("系统繁忙，请稍后重试");
    }

    try {
      InvReservationRecord existing =
          reservationRecordMapper.selectByBusinessKey(tenant, request.getBusinessKey());
      if (existing != null) {
        assertSameTenant(existing, tenantId);
        ReservationStatus status = existing.statusEnum();
        if (status == ReservationStatus.RESERVED || status == ReservationStatus.CONFIRMED) {
          log.warn("预占已存在，幂等返回: businessKey={}, status={}",
              request.getBusinessKey(), status);
          return true;
        }
        log.warn("预占已处于终态，拒绝复用业务键: businessKey={}, status={}",
            request.getBusinessKey(), status);
        return false;
      }

      Inventory inventory = inventoryMapper.selectOne(
          new LambdaQueryWrapper<Inventory>()
              .eq(Inventory::getSkuId, request.getSkuId())
              .eq(Inventory::getWarehouseId, request.getWarehouseId())
              .eq(Inventory::getDeleted, false)
      );

      if (inventory == null) {
        log.error("库存不存在 skuId={}, warehouseId={}",
            request.getSkuId(), request.getWarehouseId());
        throw new IllegalArgumentException("商品不存在");
      }

      int locked = inventoryMapper.lockStock(inventory.getId(), quantity);
      if (locked == 0) {
        log.error("库存不足: skuId={}, available={}, required={}",
            request.getSkuId(), inventory.getAvailableStock(), quantity);
        throw new IllegalArgumentException(
            String.format("库存不足: 可用 %d, 需要%d", inventory.getAvailableStock(), quantity));
      }

      InvReservationRecord record = new InvReservationRecord();
      record.setTenantId(tenant);
      record.setBusinessKey(request.getBusinessKey());
      record.setSkuId(request.getSkuId());
      record.setWarehouseId(request.getWarehouseId());
      record.setInventoryId(inventory.getId());
      record.setQuantity(quantity);
      record.setStatus(ReservationStatus.RESERVED.name());
      record.setReservedAt(LocalDateTime.now());

      int timeoutSeconds = request.getTimeoutSeconds() != null && request.getTimeoutSeconds() > 0
          ? request.getTimeoutSeconds() : DEFAULT_TIMEOUT_SECONDS;
      record.setExpireAt(LocalDateTime.now().plusSeconds(timeoutSeconds));

      reservationRecordMapper.insert(record);

      log.debug("库存预占成功: businessKey={}, quantity={}, expireAt={}",
          request.getBusinessKey(), quantity, record.getExpireAt());
      return true;
    } finally {
      lock.release();
    }
  }

  @Override
  @Master(reason = "写操作必须走主库")
  @Transactional(rollbackFor = Exception.class)
  public boolean confirmReservation(String businessKey) {
    UUID tenantId = TenantContextHolder.getRequiredTenantId();
    String tenant = tenantId.toString();

    InvReservationRecord record = reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
    if (record == null) {
      log.warn("预占不存在或不属于当前租户 businessKey={}", businessKey);
      return false;
    }
    assertSameTenant(record, tenantId);

    ReservationStatus status = record.statusEnum();
    if (status == ReservationStatus.CONFIRMED) {
      log.debug("预占已确认，幂等返回: businessKey={}", businessKey);
      return true;
    }
    if (status != ReservationStatus.RESERVED) {
      log.warn("非法状态流转（确认）: businessKey={}, status={}", businessKey, status);
      return false;
    }

    int moved = reservationRecordMapper.markConfirmed(record.getId(), tenant);
    if (moved == 0) {
      // 并发下被其它线程先流转：只有“已确认”才算幂等成功
      InvReservationRecord latest =
          reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
      boolean confirmed = latest != null && latest.statusEnum() == ReservationStatus.CONFIRMED;
      log.debug("预占确认并发竞态: businessKey={}, confirmed={}", businessKey, confirmed);
      return confirmed;
    }

    int stockUpdated = inventoryMapper.confirmStock(record.getInventoryId(), record.getQuantity());
    if (stockUpdated == 0) {
      throw new IllegalStateException("锁定库存不足，无法确认预占: businessKey=" + businessKey);
    }

    log.debug("预占确认成功: businessKey={}, quantity={}", businessKey, record.getQuantity());
    return true;
  }

  @Override
  @Master(reason = "写操作必须走主库")
  @Transactional(rollbackFor = Exception.class)
  public boolean releaseReservation(String businessKey) {
    UUID tenantId = TenantContextHolder.getRequiredTenantId();
    String tenant = tenantId.toString();

    InvReservationRecord record = reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
    if (record == null) {
      log.warn("预占不存在或不属于当前租户 businessKey={}", businessKey);
      return false;
    }
    assertSameTenant(record, tenantId);

    ReservationStatus status = record.statusEnum();
    if (status == ReservationStatus.RELEASED || status == ReservationStatus.EXPIRED) {
      log.debug("预占已释放，幂等返回: businessKey={}, status={}", businessKey, status);
      return true;
    }
    if (status == ReservationStatus.CONFIRMED) {
      log.warn("非法状态流转（释放已确认预占）: businessKey={}", businessKey);
      return false;
    }

    int moved = reservationRecordMapper.markReleased(record.getId(), tenant);
    if (moved == 0) {
      InvReservationRecord latest =
          reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
      boolean alreadyReleased = latest != null
          && (latest.statusEnum() == ReservationStatus.RELEASED
              || latest.statusEnum() == ReservationStatus.EXPIRED);
      log.debug("预占释放并发竞态: businessKey={}, alreadyReleased={}", businessKey, alreadyReleased);
      return alreadyReleased;
    }

    int stockUpdated = inventoryMapper.releaseStock(record.getInventoryId(), record.getQuantity());
    if (stockUpdated == 0) {
      throw new IllegalStateException("锁定库存不足，无法释放预占: businessKey=" + businessKey);
    }

    log.debug("预占释放成功: businessKey={}, quantity={}", businessKey, record.getQuantity());
    return true;
  }

  @Override
  @Slave
  public boolean checkReservationExists(String businessKey) {
    String tenant = TenantContextHolder.getRequiredTenantId().toString();
    InvReservationRecord record = reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
    return record != null && record.statusEnum() == ReservationStatus.RESERVED;
  }

  @Override
  @Slave
  public Integer getReservedQuantity(String businessKey) {
    String tenant = TenantContextHolder.getRequiredTenantId().toString();
    InvReservationRecord record = reservationRecordMapper.selectByBusinessKey(tenant, businessKey);
    if (record == null || record.statusEnum() != ReservationStatus.RESERVED) {
      return null;
    }
    return record.getQuantity();
  }

  @Override
  @Master(reason = "写操作必须走主库")
  @Transactional(rollbackFor = Exception.class)
  public int releaseExpiredReservations() {
    List<InvReservationRecord> expired = reservationRecordMapper.selectExpired(EXPIRY_BATCH_SIZE);
    if (expired.isEmpty()) {
      return 0;
    }

    int released = 0;
    for (InvReservationRecord record : expired) {
      String tenant = record.getTenantId();
      UUID tenantId;
      try {
        tenantId = UUID.fromString(tenant);
      } catch (RuntimeException e) {
        log.error("预占记录 tenant_id 非法，跳过: id={}, tenantId={}", record.getId(), tenant);
        continue;
      }

      Boolean done = TenantContextHolder.executeInTenantContext(tenantId,
          () -> expireOne(record, tenant));
      if (Boolean.TRUE.equals(done)) {
        released++;
      }
    }

    log.info("释放过期预占完成: scanned={}, released={}", expired.size(), released);
    return released;
  }

  private boolean expireOne(InvReservationRecord record, String tenant) {
    int moved = reservationRecordMapper.markExpired(record.getId(), tenant);
    if (moved == 0) {
      return false;
    }
    int stockUpdated = inventoryMapper.releaseStock(record.getInventoryId(), record.getQuantity());
    if (stockUpdated == 0) {
      throw new IllegalStateException(
          "锁定库存不足，无法释放过期预占: businessKey=" + record.getBusinessKey());
    }
    log.debug("过期预占已释放: businessKey={}, quantity={}",
        record.getBusinessKey(), record.getQuantity());
    return true;
  }

  private void assertSameTenant(InvReservationRecord record, UUID tenantId) {
    if (record.getTenantId() != null && !record.getTenantId().equals(tenantId.toString())) {
      log.warn("跨租户访问预占记录被拒绝: businessKey={}, ownerTenant={}, callerTenant={}",
          record.getBusinessKey(), record.getTenantId(), tenantId);
      throw new AccessDeniedException("预占记录不属于当前租户");
    }
  }
}
