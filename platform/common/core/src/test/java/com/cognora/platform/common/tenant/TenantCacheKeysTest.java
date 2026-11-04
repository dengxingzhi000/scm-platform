package com.cognora.platform.common.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 租户化缓存键的隔离性与 fail-closed 行为。
 */
class TenantCacheKeysTest {

    private static final String STOCK_SKU = "SKU-001";
    private static final String WAREHOUSE = "WH-001";

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void sameParametersProduceDifferentKeysForDifferentTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContextHolder.setTenantId(tenantA);
        String globalA = TenantCacheKeys.global("inventoryStats");
        String pagedA = TenantCacheKeys.paged("hotProducts", 1, 10);
        String stockA = TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE);

        String detailA = TenantCacheKeys.inventoryDetail(STOCK_SKU, WAREHOUSE);
        String batchA = TenantCacheKeys.inventoryBatch(null, STOCK_SKU);
        String scopedA = TenantCacheKeys.scoped("apiPermissions", "all");

        TenantContextHolder.setTenantId(tenantB);
        String globalB = TenantCacheKeys.global("inventoryStats");
        String pagedB = TenantCacheKeys.paged("hotProducts", 1, 10);
        String stockB = TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE);
        String detailB = TenantCacheKeys.inventoryDetail(STOCK_SKU, WAREHOUSE);
        String batchB = TenantCacheKeys.inventoryBatch(null, STOCK_SKU);
        String scopedB = TenantCacheKeys.scoped("apiPermissions", "all");

        assertNotEquals(globalA, globalB);
        assertNotEquals(pagedA, pagedB);
        assertNotEquals(stockA, stockB);
        assertNotEquals(detailA, detailB);
        assertNotEquals(batchA, batchB);
        assertNotEquals(scopedA, scopedB);
        assertTrue(globalA.startsWith(tenantA.toString()));
        assertTrue(globalB.startsWith(tenantB.toString()));
    }

    @Test
    void scopedAndBatchKeysWriteByTenantAAreNotReadableByTenantB() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Map<String, Object> cache = new HashMap<>();

        TenantContextHolder.setTenantId(tenantA);
        cache.put(TenantCacheKeys.scoped("apiPermissions", "all"), "api-perms-of-tenant-a");
        cache.put(TenantCacheKeys.scoped("inventory:reservation", "ORDER-001"), "reservation-a");
        cache.put(TenantCacheKeys.inventoryBatch(null, STOCK_SKU), "batch-a");
        cache.put(TenantCacheKeys.inventoryCheck(STOCK_SKU, WAREHOUSE, 1), 100);

        TenantContextHolder.setTenantId(tenantB);
        assertFalse(cache.containsKey(TenantCacheKeys.scoped("apiPermissions", "all")));
        assertFalse(cache.containsKey(TenantCacheKeys.scoped("inventory:reservation", "ORDER-001")));
        assertFalse(cache.containsKey(TenantCacheKeys.inventoryBatch(null, STOCK_SKU)));
        assertFalse(cache.containsKey(TenantCacheKeys.inventoryCheck(STOCK_SKU, WAREHOUSE, 1)));

        TenantContextHolder.setTenantId(tenantA);
        assertEquals("api-perms-of-tenant-a", cache.get(TenantCacheKeys.scoped("apiPermissions", "all")));
        assertEquals("reservation-a", cache.get(TenantCacheKeys.scoped("inventory:reservation", "ORDER-001")));
        assertEquals("batch-a", cache.get(TenantCacheKeys.inventoryBatch(null, STOCK_SKU)));
        assertEquals(100, cache.get(TenantCacheKeys.inventoryCheck(STOCK_SKU, WAREHOUSE, 1)));
    }

    @Test
    void cacheEntriesDoNotCollideAcrossTenants() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Map<String, Object> cache = new HashMap<>();

        TenantContextHolder.setTenantId(tenantA);
        cache.put(TenantCacheKeys.global("inventoryStats"), "stats-of-tenant-a");
        cache.put(TenantCacheKeys.paged("hotProducts", 1, 10), "hot-of-tenant-a");
        cache.put(TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE), 100);

        TenantContextHolder.setTenantId(tenantB);
        cache.put(TenantCacheKeys.global("inventoryStats"), "stats-of-tenant-b");
        cache.put(TenantCacheKeys.paged("hotProducts", 1, 10), "hot-of-tenant-b");
        cache.put(TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE), 7);

        assertEquals(6, cache.size(), "两个租户的同参数请求必须落在不同缓存条目");

        TenantContextHolder.setTenantId(tenantA);
        assertEquals("stats-of-tenant-a", cache.get(TenantCacheKeys.global("inventoryStats")));
        assertEquals("hot-of-tenant-a", cache.get(TenantCacheKeys.paged("hotProducts", 1, 10)));
        assertEquals(100, cache.get(TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE)));

        TenantContextHolder.setTenantId(tenantB);
        assertEquals("stats-of-tenant-b", cache.get(TenantCacheKeys.global("inventoryStats")));
        assertEquals("hot-of-tenant-b", cache.get(TenantCacheKeys.paged("hotProducts", 1, 10)));
        assertEquals(7, cache.get(TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE)));
    }

    @Test
    void inventoryKeysKeepExistingPrefixSoPatternCleanupStillWorks() {
        TenantContextHolder.setTenantId(UUID.randomUUID());

        String stock = TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE);
        String detail = TenantCacheKeys.inventoryDetail(STOCK_SKU, WAREHOUSE);

        assertTrue(stock.startsWith("inventory:stock:"), "必须保留 inventory:stock: 前缀");
        assertTrue(detail.startsWith("inventory:detail:"), "必须保留 inventory:detail: 前缀");
        assertTrue(TenantCacheKeys.inventoryBatch(null, STOCK_SKU).startsWith("inventory:batch:"));
        assertTrue(TenantCacheKeys.scoped("inventory:reservation", "ORDER-001")
                .startsWith("inventory:reservation:"));

        // 同步写入与 deleteCache 走同一个工厂，重复生成必须得到同一 key
        assertEquals(stock, TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE));
        assertEquals(detail, TenantCacheKeys.inventoryDetail(STOCK_SKU, WAREHOUSE));
    }

    @Test
    void missingTenantContextFailsClosedInsteadOfFallingBackToSharedKey() {
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.global("inventoryStats"));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.paged("hotProducts", 1, 10));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.inventoryStock(STOCK_SKU, WAREHOUSE));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.inventoryDetail(STOCK_SKU, WAREHOUSE));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.inventoryBatch(null, STOCK_SKU));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.inventoryCheck(STOCK_SKU, WAREHOUSE, 1));
        assertThrows(TenantParseException.class, () -> TenantCacheKeys.scoped("apiPermissions", "all"));
    }
}
