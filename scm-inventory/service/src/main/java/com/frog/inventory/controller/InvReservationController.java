package com.frog.inventory.controller;

import com.frog.inventory.domain.dto.InventoryReservationRequest;
import com.frog.inventory.service.IInvReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存预占控制�?
 *
 * <p>提供库存预占、确认、释放等REST API接口
 *
 * <p>业务场景�?
 * <ul>
 *   <li>订单创建时预占库存（POST /reserve�?/li>
 *   <li>支付成功后确认扣减（PUT /confirm/{businessKey}�?/li>
 *   <li>订单取消/超时后释放库存（DELETE /release/{businessKey}�?/li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/reservation")
public class InvReservationController {

  private final IInvReservationService reservationService;

  /**
   * 预占库存（订单创建时调用�?
   */
  @PostMapping("/reserve")
  public boolean reserveInventory(
      @RequestBody @Valid InventoryReservationRequest request) {

    log.info("🔵 [API] 预占库存: skuId={}, warehouseId={}, quantity={}, businessKey={}",
        request.getSkuId(), request.getWarehouseId(),
        request.getQuantity(), request.getBusinessKey());

    try {
      boolean success = reservationService.reserveInventory(request);

      if (success) {
        log.info("�?[API] 库存预占成功: businessKey={}, quantity={}",
            request.getBusinessKey(), request.getQuantity());
      } else {
        log.warn("⚠️  [API] 库存预占失败: businessKey={}", request.getBusinessKey());
      }

      return success;

    } catch (IllegalArgumentException e) {
      log.error("�?[API] 库存预占失败（参数错误）: {}", e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      log.error("�?[API] 库存预占失败（业务异常）: businessKey={}, error={}",
          request.getBusinessKey(), e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("�?[API] 库存预占异常: businessKey={}, error={}",
          request.getBusinessKey(), e.getMessage(), e);
      throw new RuntimeException("库存预占失败: " + e.getMessage(), e);
    }
  }

  /**
   * 确认预占（订单支付成功后调用�?
   */
  @PutMapping("/confirm/{businessKey}")
  public boolean confirmReservation(
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🟢 [API] 确认预占: businessKey={}", businessKey);

    try {
      boolean success = reservationService.confirmReservation(businessKey);

      if (success) {
        log.info("🟢[API] 预占确认成功: businessKey={}", businessKey);
      } else {
        log.warn("⚠️  [API] 预占确认失败（预占不存在或已过期）", businessKey);
      }

      return success;

    } catch (RuntimeException e) {
      log.error("❌[API] 预占确认失败: businessKey={}, error={}",
          businessKey, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌[API] 预占确认异常: businessKey={}, error={}",
          businessKey, e.getMessage(), e);
      throw new RuntimeException("预占确认失败: " + e.getMessage(), e);
    }
  }

  /**
   * 释放预占（订单取消或超时后调用）
   */
  @DeleteMapping("/release/{businessKey}")
  public boolean releaseReservation(
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🔴 [API] 释放预占: businessKey={}", businessKey);

    try {
      boolean success = reservationService.releaseReservation(businessKey);

      if (success) {
        log.info("🟢[API] 预占释放成功: businessKey={}", businessKey);
      } else {
        log.warn("⚠️  [API] 预占释放失败（预占不存在）", businessKey);
      }

      return success;

    } catch (RuntimeException e) {
      log.error("❌[API] 预占释放失败: businessKey={}, error={}",
          businessKey, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌[API] 预占释放异常: businessKey={}, error={}",
          businessKey, e.getMessage(), e);
      throw new RuntimeException("预占释放失败: " + e.getMessage(), e);
    }
  }

  /**
   * 检查预占是否存�?
   */
  @GetMapping("/check/{businessKey}")
  public boolean checkReservationExists(
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 检查预占: businessKey={}", businessKey);

    boolean exists = reservationService.checkReservationExists(businessKey);

    log.debug("📦 [API] 预占检查结果: businessKey={}, exists={}", businessKey, exists);

    return exists;
  }

  /**
   * 获取预占的数据
   */
  @GetMapping("/quantity/{businessKey}")
  public Integer getReservedQuantity(
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 查询预占数量: businessKey={}", businessKey);

    Integer quantity = reservationService.getReservedQuantity(businessKey);

    if (quantity != null) {
      log.debug("📦 [API] 预占数量: businessKey={}, quantity={}", businessKey, quantity);
    } else {
      log.debug("⚠️  [API] 预占不存在", businessKey);
    }

    return quantity;
  }

  /**
   * 释放过期的预占（定时任务调用）
   */
  @PostMapping("/release-expired")
  public int releaseExpiredReservations() {
    log.info("🔄 [API] 开始释放过期预占");

    int count = reservationService.releaseExpiredReservations();

    log.info("✅[API] 释放过期预占完成: count={}", count);

    return count;
  }
}