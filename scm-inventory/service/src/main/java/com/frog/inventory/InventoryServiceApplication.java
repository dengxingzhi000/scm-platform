package com.frog.inventory;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 库存服务启动类
 *
 * <p>库存服务负责：
 * <ul>
 *     <li>库存管理（查询、调整、转移）</li>
 *     <li>库存预占与释放（订单场景）</li>
 *     <li>库存告警（低库存、缺货）</li>
 *     <li>库存快照（每日统计）</li>
 *     <li>库存日志（审计追踪）</li>
 *     <li>Redis 缓存与 Lua 脚本防超卖</li>
 *     <li>分布式锁（库存并发控制）</li>
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@SpringBootApplication(scanBasePackages = {"com.frog.inventory", "com.frog.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableTransactionManagement
@MapperScan("com.frog.inventory.mapper")
public class InventoryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
    System.out.println("""

        ========================================
        ✓ 库存服务启动成功 (Inventory Service)
        ========================================
        服务端口: 8202
        API 文档: http://localhost:8202/doc.html
        健康检查: http://localhost:8202/actuator/health
        ========================================
        功能特性:
        - 库存 CRUD 与分页查询
        - 库存调整与仓库间调拨
        - Redis 缓存 + Lua 脚本防超卖
        - 分布式锁（并发控制）
        - TCC 分布式事务支持
        ========================================
        """);
  }
}