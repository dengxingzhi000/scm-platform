package com.scmcloud.common.tenant.quota;

import lombok.Getter;

/**
 * Quota type enum
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Getter
public enum QuotaType {

    /**
     * User Count
     */
    USERS("User Count", "current_users", "max_users"),

    /**
     * Warehouse Count
     */
    WAREHOUSES("Warehouse Count", "current_warehouses", "max_warehouses"),

    /**
     * SKU Count
     */
    SKUS("SKU Count", "current_skus", "max_skus"),

    /**
     * Daily Orders
     */
    ORDERS("Daily Orders", "current_orders_today", "max_orders_per_day"),

    /**
     * Storage (GB)
     */
    STORAGE("Storage (GB)", "current_storage_gb", "max_storage_gb"),

    /**
     * Daily API Calls
     */
    API_CALLS("Daily API Calls", "current_api_calls_today", "max_api_calls_per_day");

    /**
     * Description
     */
    private final String description;

    /**
     * Current usage field name (database column)
     */
    private final String currentField;

    /**
     * Max limit field name (database column)
     */
    private final String maxField;

    QuotaType(String description, String currentField, String maxField) {
        this.description = description;
        this.currentField = currentField;
        this.maxField = maxField;
    }
}