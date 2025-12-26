package com.frog.inventory.service;

import com.frog.inventory.domain.dto.InventoryReservationRequest;

/**
 * 库存预占服务接口
 *
 * <p>提供基于 Redis 的轻量级库存预占机制，适用于高并发订单场景
 *
 * <p>与 TCC 服务的区别：
 * <ul>
 *   <li>TCC：基于 Seata 的分布式事务，保证强一致性，适合跨服务事务</li>
 *   <li>Redis 预占：基于 Redis 的轻量级预占，性能更高，适合单服务场景</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
public interface IInvReservationService {

  /**
   * 预占库存（锁定库存，设置过期时间）
   *
   * <p>业务场景：订单创建时预占库存，防止超卖
   *
   * <p>实现逻辑：
   * <ol>
   *   <li>检查库存是否充足</li>
   *   <li>将可用库存转为锁定库存（available_stock - X, locked_stock + X）</li>
   *   <li>在 Redis 中记录预占信息（包含业务键、数量、过期时间）</li>
   *   <li>设置自动过期时间（默认15分钟）</li>
   * </ol>
   *
   * @param request 预占请求
   * @return true-预占成功，false-预占失败（库存不足或重复预占）
   */
  boolean reserveInventory(InventoryReservationRequest request);

  /**
   * 确认预占（支付成功后，将锁定库存扣减）
   *
   * <p>业务场景：订单支付成功后，确认扣减库存
   *
   * <p>实现逻辑：
   * <ol>
   *   <li>检查预占记录是否存在且未过期</li>
   *   <li>扣减锁定库存（locked_stock - X, total_stock - X）</li>
   *   <li>删除 Redis 预占记录</li>
   * </ol>
   *
   * @param businessKey 业务键（订单号）
   * @return true-确认成功，false-确认失败（预占不存在或已过期）
   */
  boolean confirmReservation(String businessKey);

  /**
   * 释放预占（订单取消或超时，释放锁定库存）
   *
   * <p>业务场景：订单取消或支付超时，释放已锁定的库存
   *
   * <p>实现逻辑：
   * <ol>
   *   <li>检查预占记录是否存在</li>
   *   <li>释放锁定库存为可用库存（available_stock + X, locked_stock - X）</li>
   *   <li>删除 Redis 预占记录</li>
   * </ol>
   *
   * @param businessKey 业务键（订单号）
   * @return true-释放成功，false-释放失败（预占不存在）
   */
  boolean releaseReservation(String businessKey);

  /**
   * 检查预占是否存在且未过期
   *
   * @param businessKey 业务键（订单号）
   * @return true-预占存在且有效，false-预占不存在或已过期
   */
  boolean checkReservationExists(String businessKey);

  /**
   * 获取预占的数量
   *
   * @param businessKey 业务键（订单号）
   * @return 预占的数量，如果不存在则返回 null
   */
  Integer getReservedQuantity(String businessKey);

  /**
   * 自动释放过期的预占（由定时任务调用）
   *
   * <p>Redis 的过期机制可能有延迟，需要定时任务兜底扫描过期预占并释放库存
   *
   * @return 释放的预占数量
   */
  int releaseExpiredReservations();
}