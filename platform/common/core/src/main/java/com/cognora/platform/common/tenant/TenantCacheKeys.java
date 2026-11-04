package com.cognora.platform.common.tenant;

import java.util.UUID;

/**
 * 统一的租户化缓存键工厂。
 *
 * <p>所有跨租户可见的缓存键都必须经过这里生成，避免出现 {@code 'global'}、
 * {@code page_size}、{@code inventory:stock:{sku}} 这类不带租户的键，
 * 导致两个租户共用同一条缓存记录。</p>
 *
 * <p>用法（Spring Cache SpEL）：</p>
 * <pre>
 * &#64;Cacheable(value = "inventoryStats",
 *            key = "T(com.cognora.platform.common.tenant.TenantCacheKeys).global('inventoryStats')")
 * </pre>
 *
 * <p>缺少租户上下文时直接抛出 {@link TenantParseException}（fail-closed），
 * 不会退化成公共键。</p>
 */
public final class TenantCacheKeys {

    private TenantCacheKeys() {
    }

    /** 全局单值缓存键，如库存统计。 */
    public static String global(String namespace) {
        return currentTenantId() + ":" + namespace;
    }

    /** 分页缓存键，如热门商品 / 最新商品。 */
    public static String paged(String namespace, Object page, Object size) {
        return currentTenantId() + ":" + namespace + ":" + page + "_" + size;
    }

    /** 库存数量键，保持 {@code inventory:stock:} 前缀以便既有按前缀清理仍然有效。 */
    public static String inventoryStock(String skuId, String warehouseId) {
        return "inventory:stock:" + currentTenantId() + ":" + skuId + ":" + warehouseId;
    }

    /** 库存详情键。 */
    public static String inventoryDetail(String skuId, String warehouseId) {
        return "inventory:detail:" + currentTenantId() + ":" + skuId + ":" + warehouseId;
    }

    /** 批量库存键（warehouse 为空时用 ALL，与旧键语义一致）。 */
    public static String inventoryBatch(Object warehouseId, Object skuIds) {
        return "inventory:batch:" + currentTenantId() + ":"
                + (warehouseId == null ? "ALL" : warehouseId) + ":"
                + (skuIds == null ? "" : skuIds);
    }

    /** 库存可用性检查键。 */
    public static String inventoryCheck(String skuId, String warehouseId, Object quantity) {
        return inventoryDetail(skuId, warehouseId) + ":available:" + quantity;
    }

    /**
     * 通用业务键：{@code namespace:tenantId:parts}。
     *
     * <p>Redis 手写键统一使用该约定；中间件前缀（如 {@code inventory:stock:}）由各自的
     * 专用工厂方法负责，避免调用方直接拼接字符串。</p>
     */
    public static String scoped(String namespace, Object... parts) {
        StringBuilder key = new StringBuilder(namespace).append(':').append(currentTenantId());
        if (parts != null) {
            for (Object part : parts) {
                key.append(':').append(part);
            }
        }
        return key.toString();
    }

    private static String currentTenantId() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return tenantId.toString();
    }
}
