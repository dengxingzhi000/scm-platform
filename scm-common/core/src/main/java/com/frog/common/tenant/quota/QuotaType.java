package com.frog.common.tenant.quota;

import lombok.Getter;

/**
 * 配额类型枚举
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Getter
public enum QuotaType {

    /**
     * 用户数
     */
    USERS("用户数", "current_users", "max_users"),

    /**
     * 仓库数
     */
    WAREHOUSES("仓库数", "current_warehouses", "max_warehouses"),

    /**
     * SKU数
     */
    SKUS("SKU数", "current_skus", "max_skus"),

    /**
     * 每日订单数
     */
    ORDERS("每日订单数", "current_orders_today", "max_orders_per_day"),

    /**
     * 存储空间（GB）
     */
    STORAGE("存储空间(GB)", "current_storage_gb", "max_storage_gb"),

    /**
     * 每日API调用数
     */
    API_CALLS("每日API调用数", "current_api_calls_today", "max_api_calls_per_day");

    /**
     * 描述
     */
    private final String description;

    /**
     * 当前使用量字段名（数据库字段）
     */
    private final String currentField;

    /**
     * 最大限额字段名（数据库字段）
     */
    private final String maxField;

    QuotaType(String description, String currentField, String maxField) {
        this.description = description;
        this.currentField = currentField;
        this.maxField = maxField;
    }
}