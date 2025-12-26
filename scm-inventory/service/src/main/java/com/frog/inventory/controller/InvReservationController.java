package com.frog.inventory.controller;

import com.frog.inventory.domain.dto.InventoryReservationRequest;
import com.frog.inventory.service.IInvReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 库存预占控制器
 *
 * <p>提供库存预占、确认、释放等REST API接口
 *
 * <p>业务场景：
 * <ul>
 *   <li>订单创建时预占库存（POST /reserve）</li>
 *   <li>支付成功后确认扣减（PUT /confirm/{businessKey}）</li>
 *   <li>订单取消/超时后释放库存（DELETE /release/{businessKey}）</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/reservation")
@Tag(name = "库存预占", description = "库存预占、确认、释放接口（订单场景）")
public class InvReservationController {

  @Autowired
  private IInvReservationService reservationService;

  /**
   * 预占库存（订单创建时调用）
   */
  @PostMapping("/reserve")
  @Operation(summary = "预占库存", description = "订单创建时预占库存，防止超卖（锁定库存15分钟）")
  public boolean reserveInventory(
      @Parameter(description = "预占请求")
      @RequestBody @Valid InventoryReservationRequest request) {

    log.info("🔵 [API] 预占库存: skuId={}, warehouseId={}, quantity={}, businessKey={}",
        request.getSkuId(), request.getWarehouseId(),
        request.getQuantity(), request.getBusinessKey());

    try {
      boolean success = reservationService.reserveInventory(request);

      if (success) {
        log.info("✅ [API] 库存预占成功: businessKey={}, quantity={}",
            request.getBusinessKey(), request.getQuantity());
      } else {
        log.warn("⚠️  [API] 库存预占失败: businessKey={}", request.getBusinessKey());
      }

      return success;

    } catch (IllegalArgumentException e) {
      log.error("❌ [API] 库存预占失败（参数错误）: {}", e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      log.error("❌ [API] 库存预占失败（业务异常）: businessKey={}, error={}",
          request.getBusinessKey(), e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ [API] 库存预占异常: businessKey={}, error={}",
          request.getBusinessKey(), e.getMessage(), e);
      throw new RuntimeException("库存预占失败: " + e.getMessage(), e);
    }
  }

  /**
   * 确认预占（订单支付成功后调用）
   */
  @PutMapping("/confirm/{businessKey}")
  @Operation(summary = "确认预占", description = "订单支付成功后，确认扣减已预占的库存")
  public boolean confirmReservation(
      @Parameter(description = "业务键（订单号）", required = true)
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🟢 [API] 确认预占: businessKey={}", businessKey);

    try {
      boolean success = reservationService.confirmReservation(businessKey);

      if (success) {
        log.info("✅ [API] 预占确认成功: businessKey={}", businessKey);
      } else {
        log.warn("⚠️  [API] 预占确认失败（预占不存在或已过期）: businessKey={}", businessKey);
      }

      return success;

    } catch (RuntimeException e) {
      log.error("❌ [API] 预占确认失败: businessKey={}, error={}",
          businessKey, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ [API] 预占确认异常: businessKey={}, error={}",
          businessKey, e.getMessage(), e);
      throw new RuntimeException("预占确认失败: " + e.getMessage(), e);
    }
  }

  /**
   * 释放预占（订单取消或超时后调用）
   */
  @DeleteMapping("/release/{businessKey}")
  @Operation(summary = "释放预占", description = "订单取消或支付超时后，释放已预占的库存")
  public boolean releaseReservation(
      @Parameter(description = "业务键（订单号）", required = true)
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🔴 [API] 释放预占: businessKey={}", businessKey);

    try {
      boolean success = reservationService.releaseReservation(businessKey);

      if (success) {
        log.info("✅ [API] 预占释放成功: businessKey={}", businessKey);
      } else {
        log.warn("⚠️  [API] 预占释放失败（预占不存在）: businessKey={}", businessKey);
      }

      return success;

    } catch (RuntimeException e) {
      log.error("❌ [API] 预占释放失败: businessKey={}, error={}",
          businessKey, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error("❌ [API] 预占释放异常: businessKey={}, error={}",
          businessKey, e.getMessage(), e);
      throw new RuntimeException("预占释放失败: " + e.getMessage(), e);
    }
  }

  /**
   * 检查预占是否存在
   */
  @GetMapping("/check/{businessKey}")
  @Operation(summary = "检查预占", description = "检查指定业务键的预占是否存在且未过期")
  public boolean checkReservationExists(
      @Parameter(description = "业务键（订单号）", required = true)
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 检查预占: businessKey={}", businessKey);

    boolean exists = reservationService.checkReservationExists(businessKey);

    log.debug("📦 [API] 预占检查结果: businessKey={}, exists={}", businessKey, exists);

    return exists;
  }

  /**
   * 获取预占的数量
   */
  @GetMapping("/quantity/{businessKey}")
  @Operation(summary = "获取预占数量", description = "查询指定业务键预占的库存数量")
  public Integer getReservedQuantity(
      @Parameter(description = "业务键（订单号）", required = true)
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 查询预占数量: businessKey={}", businessKey);

    Integer quantity = reservationService.getReservedQuantity(businessKey);

    if (quantity != null) {
      log.debug("📦 [API] 预占数量: businessKey={}, quantity={}", businessKey, quantity);
    } else {
      log.debug("⚠️  [API] 预占不存在: businessKey={}", businessKey);
    }

    return quantity;
  }

  /**
   * 释放过期的预占（定时任务调用）
   */
  @PostMapping("/release-expired")
  @Operation(summary = "释放过期预占", description = "定时任务调用，释放已过期的预占记录")
  public int releaseExpiredReservations() {
    log.info("🔄 [API] 开始释放过期预占");

    int count = reservationService.releaseExpiredReservations();

    log.info("✅ [API] 释放过期预占完成: count={}", count);

    return count;
  }
}