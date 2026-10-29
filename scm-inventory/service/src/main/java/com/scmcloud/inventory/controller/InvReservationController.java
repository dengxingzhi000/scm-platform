package com.scmcloud.inventory.controller;

import com.scmcloud.inventory.dto.InventoryReservationRequest;
import com.scmcloud.inventory.service.IInvReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/reservation")
@Tag(name = "库存预占", description = "提供库存预占、确认、释放及预占状态查询接口。")
public class InvReservationController {

  private final IInvReservationService reservationService;

  /**
   * 预占库存（订单创建时调用）
   */
  @Operation(summary = "预占库存", description = "订单创建时按SKU、仓库和数量预占库存并生成预占记录。")
  @PostMapping("/reserve")
  public boolean reserveInventory(
      @RequestBody @Valid InventoryReservationRequest request) {

    log.info("🔵 [API] 预占库存: skuId={}, warehouseId={}, quantity={}, businessKey={}",
        (request.getSkuId() + "").replace("\r", "_").replace("\n", "_"), (request.getWarehouseId() + "").replace("\r", "_").replace("\n", "_"),
        (request.getQuantity() + "").replace("\r", "_").replace("\n", "_"), (request.getBusinessKey() + "").replace("\r", "_").replace("\n", "_"));

    try {
      boolean success = reservationService.reserveInventory(request);

      if (success) {
        log.info("✅[API] 库存预占成功: businessKey={}, quantity={}",
            (request.getBusinessKey() + "").replace("\r", "_").replace("\n", "_"), (request.getQuantity() + "").replace("\r", "_").replace("\n", "_"));
      } else {
        log.warn("⚠️  [API] 库存预占失败: businessKey={}", (request.getBusinessKey() + "").replace("\r", "_").replace("\n", "_"));
      }

      return success;

    } catch (IllegalArgumentException e) {
      log.error("❌[API] 库存预占失败（参数错误）: {}", e.getMessage());
      throw e;
    } catch (RuntimeException e) {
      log.error("❌[API] 库存预占失败（业务异常）: businessKey={}, error={}",
          (request.getBusinessKey() + "").replace("\r", "_").replace("\n", "_"), (e.getMessage() + "").replace("\r", "_").replace("\n", "_"));
      throw e;
    } catch (Exception e) {
      log.error("❌[API] 库存预占异常: businessKey={}, error={}",
          (request.getBusinessKey() + "").replace("\r", "_").replace("\n", "_"), (e.getMessage() + "").replace("\r", "_").replace("\n", "_"), e);
      throw new RuntimeException("库存预占失败: " + e.getMessage(), e);
    }
  }

  /**
   * 确认预占（订单支付成功后调用）
   */
  @Operation(summary = "确认预占", description = "订单支付成功后确认预占，将预占库存转为实际扣减。")
  @PutMapping("/confirm/{businessKey}")
  public boolean confirmReservation(
      @Parameter(description = "业务键，通常为订单号，用于定位预占记录") @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🟢 [API] 确认预占: businessKey={}", (businessKey + "").replace("\r", "_").replace("\n", "_"));

    try {
      boolean success = reservationService.confirmReservation(businessKey);

      if (success) {
        log.info("🟢[API] 预占确认成功: businessKey={}", (businessKey + "").replace("\r", "_").replace("\n", "_"));
      } else {
        log.warn("⚠️  [API] 预占确认失败（预占不存在或已过期）: businessKey={}",
            businessKey == null ? null : businessKey.replace("\r", "_").replace("\n", "_"));
      }

      return success;

    } catch (RuntimeException e) {
      log.error("❌[API] 预占确认失败: businessKey={}, error={}",
          (businessKey + "").replace("\r", "_").replace("\n", "_"), (e.getMessage() + "").replace("\r", "_").replace("\n", "_"));
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
  @Operation(summary = "释放预占", description = "订单取消或超时后释放预占，将库存归还为可用库存。")
  @DeleteMapping("/release/{businessKey}")
  public boolean releaseReservation(
      @Parameter(description = "业务键，通常为订单号，用于定位预占记录") @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.info("🔴 [API] 释放预占: businessKey={}", businessKey);

    try {
      boolean success = reservationService.releaseReservation(businessKey);

      if (success) {
        log.info("🟢[API] 预占释放成功: businessKey={}", businessKey);
      } else {
        log.warn("⚠️  [API] 预占释放失败（预占不存在）: businessKey={}",
            businessKey == null ? null : businessKey.replace("\r", "_").replace("\n", "_"));
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
   * 检查预占是否存在
   */
  @Operation(summary = "检查预占是否存在", description = "根据业务键判断对应的预占记录是否仍然存在。")
  @GetMapping("/check/{businessKey}")
  public boolean checkReservationExists(
      @Parameter(description = "业务键，通常为订单号，用于定位预占记录") @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 检查预占 businessKey={}", businessKey);

    boolean exists = reservationService.checkReservationExists(businessKey);

    log.debug("📦 [API] 预占检查结果 businessKey={}, exists={}", businessKey, exists);

    return exists;
  }

  /**
   * 获取预占的数据
   */
  @Operation(summary = "查询预占数量", description = "根据业务键查询对应预占记录占用的库存数量。")
  @GetMapping("/quantity/{businessKey}")
  public Integer getReservedQuantity(
      @Parameter(description = "业务键，通常为订单号，用于定位预占记录") @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {

    log.debug("📦 [API] 查询预占数量: businessKey={}", businessKey);

    Integer quantity = reservationService.getReservedQuantity(businessKey);

    if (quantity != null) {
      log.debug("📦 [API] 预占数量: businessKey={}, quantity={}", (businessKey + "").replace("\r", "_").replace("\n", "_"), (quantity + "").replace("\r", "_").replace("\n", "_"));
    } else {
      log.debug("⚠️  [API] 预占不存在: businessKey={}",
          businessKey == null ? null : businessKey.replace("\r", "_").replace("\n", "_"));
    }

    return quantity;
  }

  /**
   * 释放过期的预占（定时任务调用）
   */
  @Operation(summary = "释放过期预占", description = "批量释放已超过保留期限的预占记录，供定时任务调用。")
  @PostMapping("/release-expired")
  public int releaseExpiredReservations() {
    log.info("🔄 [API] 开始释放过期预占");

    int count = reservationService.releaseExpiredReservations();

    log.info("✅[API] 释放过期预占完成: count={}", count);

    return count;
  }
}