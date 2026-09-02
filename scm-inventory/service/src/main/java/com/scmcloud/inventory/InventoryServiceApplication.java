package com.scmcloud.inventory;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 搴撳瓨鏈嶅姟鍚姩锟?
 *
 * <p>搴撳瓨鏈嶅姟璐負矗锟?
 * <ul>
 *     <li>搴撳瓨绠＄悊锟堟煡璇€佽皟鏁淬€佽浆绉伙級</li>
 *     <li>搴撳瓨棰勫崰涓庨噴鏀撅紙璁㈠崟鍦烘櫙锟?/li>
 *     <li>搴撳瓨鍛婅锛堜綆搴撳瓨銆佺己璐э級</li>
 *     <li>搴撳瓨蹇収锛庢瘡鏃ョ粺璁★級</li>
 *     <li>搴撳瓨鏃ュ織锛庡畨鍏ㄨ拷韪級</li>
 *     <li>Redis 缂撳瓨锟絃ua 鑴氭湰闃茶秴锟?/li>
 *     <li>鍒嗗竷寮忛攣锟?
 * </ul>
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@SpringBootApplication(scanBasePackages = {"com.scmcloud.inventory", "com.scmcloud.common"})
@EnableDiscoveryClient
@EnableDubbo
@EnableScheduling
@EnableTransactionManagement
@MapperScan("com.scmcloud.inventory.mapper")
public class InventoryServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
    System.out.println("""

        ========================================
        锟藉簱瀛樻湇鍔″惎鍔ㄦ垚鍔?(Inventory Service)
        ========================================
        鏈嶅姟绔彛: 8202
        API 鏂囨。: http://localhost:8202/doc.html
        鍋ュ悍妫€锟?http://localhost:8202/actuator/health
        ========================================
        鍔熻兘鐗癸拷
        - 搴撳瓨 CRUD 涓庡垎椤垫煡锟?
        - 搴撳瓨璋冩暣涓庝粨搴撻棿璋冩嫧
        - Redis 缂撳瓨 + Lua 鑴氭湰闃茶秴锟?
        - 鍒嗗竷寮忛攣锛堝苟鍙戞帶鍒讹級
        - TCC 鍒嗗竷寮忎簨鍔℃敮锟?
        ========================================
        """);
  }
}