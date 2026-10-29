package com.scmcloud.common.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Tenant identifier value object. Immutable, non-null, type-safe.
 *
 * <p>Replaces raw {@code UUID} / {@code String} for tenant ID fields.
 * Provides compile-time distinction from other UUIDs (user IDs, role IDs, etc.).</p>
 *
 * <pre>
 * TenantId tenantId = TenantId.from(uuid);
 * TenantId tenantId = TenantId.fromString("550e8400-e29b-41d4-a716-446655440000");
 * UUID raw = tenantId.toUUID();
 * </pre>
 */
public final class TenantId implements Comparable<TenantId>, Serializable {

    private final UUID value;

    private TenantId(UUID value) {
        this.value = Objects.requireNonNull(value, "TenantId must not be null");
    }

    public static TenantId from(UUID value) {
        return new TenantId(value);
    }

    public static TenantId fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId string must not be null or blank");
        }
        return new TenantId(UUID.fromString(value));
    }

    public static TenantId generate() {
        return new TenantId(UUID.randomUUID());
    }

    public UUID toUUID() {
        return value;
    }

    /**
     * For MyBatis-Plus / database mapping (String column).
     */
    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantId tenantId)) return false;
        return value.equals(tenantId.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public int compareTo(TenantId other) {
        if (other == null) return 1;
        return this.value.compareTo(other.value);
    }
}
