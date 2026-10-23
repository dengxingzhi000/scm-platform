package com.scmcloud.analytics.materialization;

import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.analytics.query.ClickHouseExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Materializes the ODS → DWD → DWS → ADS chain for one day at a time.
 *
 * <p>All jobs are idempotent: rows for the target {@code tenant_id + date}
 * are deleted before the aggregate is re-inserted. XXL-Job sharding is applied
 * by hashing tenant ids, so each executor instance owns a deterministic
 * subset of tenants.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterializationService {

    private static final String ODS_ORDER = "analytics.ods_order";
    private static final String ODS_INVENTORY = "analytics.ods_inventory";
    private static final String DWD_SALES = "analytics.dwd_sales_detail";
    private static final String DWS_SALES = "analytics.dws_sales_day";
    private static final String DWS_INVENTORY = "analytics.dws_inventory_day";
    private static final String ADS_SALES = "analytics.ads_sales_dashboard";
    private static final String ADS_INVENTORY = "analytics.ads_inventory_health";

    private final ClickHouseExecutor executor;
    private final ClickHouseProperties properties;

    public int materializeDwd(LocalDate date, int shardIndex, int shardTotal) {
        List<String> tenants = shard(resolveTenants(date, ODS_ORDER), shardIndex, shardTotal);
        for (String tenantId : tenants) {
            executor.execute(deleteByTenantAndDate(DWD_SALES, tenantId, "pay_time", date),
                    properties.getQueryTimeoutSeconds());
            executor.execute(dwdInsert(tenantId, date), properties.getQueryTimeoutSeconds());
        }
        log.info("DWD materialization finished: date={}, tenants={}", date, tenants.size());
        return tenants.size();
    }

    public int materializeDws(LocalDate date, int shardIndex, int shardTotal) {
        List<String> tenants = shard(resolveTenants(date, ODS_ORDER + "," + ODS_INVENTORY),
                shardIndex, shardTotal);
        for (String tenantId : tenants) {
            executor.execute(deleteByTenantAndDate(DWS_SALES, tenantId, "stat_date", date),
                    properties.getQueryTimeoutSeconds());
            executor.execute(dwsSalesInsert(tenantId, date), properties.getQueryTimeoutSeconds());
            executor.execute(deleteByTenantAndDate(DWS_INVENTORY, tenantId, "stat_date", date),
                    properties.getQueryTimeoutSeconds());
            executor.execute(dwsInventoryInsert(tenantId, date), properties.getQueryTimeoutSeconds());
        }
        log.info("DWS materialization finished: date={}, tenants={}", date, tenants.size());
        return tenants.size();
    }

    public int materializeAds(LocalDate date, int shardIndex, int shardTotal) {
        List<String> tenants = shard(resolveTenants(date, ODS_ORDER + "," + ODS_INVENTORY),
                shardIndex, shardTotal);
        for (String tenantId : tenants) {
            executor.execute(deleteByTenantAndDate(ADS_SALES, tenantId, "stat_date", date),
                    properties.getQueryTimeoutSeconds());
            executor.execute(adsSalesInsert(tenantId, date), properties.getQueryTimeoutSeconds());
            executor.execute(deleteByTenantAndDate(ADS_INVENTORY, tenantId, "stat_date", date),
                    properties.getQueryTimeoutSeconds());
            executor.execute(adsInventoryInsert(tenantId, date), properties.getQueryTimeoutSeconds());
        }
        log.info("ADS materialization finished: date={}, tenants={}", date, tenants.size());
        return tenants.size();
    }

    public static LocalDate resolveDate(String jobParam) {
        if (jobParam == null || jobParam.isBlank()) {
            return LocalDate.now(ZoneOffset.UTC).minusDays(1);
        }
        String value = jobParam.trim().split(",")[0].trim();
        if (value.startsWith("date=")) {
            value = value.substring("date=".length());
        }
        return LocalDate.parse(value);
    }

    static List<String> shard(List<String> tenants, int shardIndex, int shardTotal) {
        if (shardTotal <= 1) {
            return tenants;
        }
        if (shardIndex < 0 || shardIndex >= shardTotal) {
            throw new IllegalArgumentException("Invalid shard index " + shardIndex + "/" + shardTotal);
        }
        return IntStream.range(0, tenants.size())
                .filter(i -> Math.floorMod(tenants.get(i).hashCode(), shardTotal) == shardIndex)
                .mapToObj(tenants::get)
                .collect(Collectors.toList());
    }

    private List<String> resolveTenants(LocalDate date, String tableCsv) {
        String sql = "SELECT DISTINCT tenant_id FROM (" +
                java.util.Arrays.stream(tableCsv.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .map(table -> "SELECT tenant_id FROM " + table
                                + " WHERE toDate(create_time) = " + dateLiteral(date))
                        .collect(Collectors.joining(" UNION ALL ")) +
                ")";
        Set<String> tenants = new LinkedHashSet<>();
        List<Map<String, Object>> rows = executor.query(sql, List.of("tenant_id"),
                properties.getQueryTimeoutSeconds());
        for (Map<String, Object> row : rows) {
            Object value = row.get("tenant_id");
            if (value == null) {
                continue;
            }
            try {
                tenants.add(UUID.fromString(value.toString()).toString());
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping non-UUID tenant id in {}: {}", tableCsv, value);
            }
        }
        List<String> result = new ArrayList<>(tenants);
        log.debug("Resolved {} tenants from {} for {}", result.size(), tableCsv, date);
        return result;
    }

    private String deleteByTenantAndDate(String table, String tenantId, String dateColumn, LocalDate date) {
        return "DELETE FROM " + table + " WHERE tenant_id = " + quote(tenantId)
                + " AND " + dateColumn + " = " + dateLiteral(date);
    }

    private String dwdInsert(String tenantId, LocalDate date) {
        return "INSERT INTO " + DWD_SALES + " (" +
                "tenant_id, order_no, order_item_id, sku_id, product_name, category_id, category_name, " +
                "quantity, unit_price, amount, discount_amount, pay_amount, pay_time, order_status, " +
                "user_id, create_time, update_time) " +
                "SELECT tenant_id, order_no, concat(order_no, '-1'), '', '', '', '', " +
                "1, total_amount, total_amount, 0, ifNull(pay_amount, total_amount), " +
                "if(pay_time = toDateTime(0), create_time, pay_time), ifNull(order_status, ''), " +
                "ifNull(user_id, ''), create_time, update_time " +
                "FROM " + ODS_ORDER + " FINAL WHERE tenant_id = " + quote(tenantId) +
                " AND toDate(create_time) = " + dateLiteral(date);
    }

    private String dwsSalesInsert(String tenantId, LocalDate date) {
        return "INSERT INTO " + DWS_SALES + " (" +
                "tenant_id, stat_date, category_id, sku_id, order_cnt, sales_qty, sales_amount, " +
                "pay_amount, discount_amount) " +
                "SELECT tenant_id, toDate(pay_time), category_id, sku_id, " +
                "count(DISTINCT order_no), sum(quantity), sum(amount), sum(pay_amount), " +
                "sum(discount_amount) " +
                "FROM " + DWD_SALES + " WHERE tenant_id = " + quote(tenantId) +
                " AND toDate(pay_time) = " + dateLiteral(date) +
                " GROUP BY tenant_id, toDate(pay_time), category_id, sku_id";
    }

    private String dwsInventoryInsert(String tenantId, LocalDate date) {
        return "INSERT INTO " + DWS_INVENTORY + " (" +
                "tenant_id, stat_date, warehouse_id, sku_id, opening_qty, closing_qty, inbound_qty, " +
                "outbound_qty, adjust_qty, reserved_qty) " +
                "SELECT tenant_id, toDate(create_time), warehouse_id, sku_id, " +
                "argMax(after_qty, update_time) - sumIf(quantity, change_type = 'INBOUND') " +
                "+ sumIf(quantity, change_type = 'OUTBOUND') - sumIf(quantity, change_type = 'ADJUST'), " +
                "argMax(after_qty, update_time), " +
                "sumIf(quantity, change_type = 'INBOUND'), " +
                "sumIf(quantity, change_type = 'OUTBOUND'), " +
                "sumIf(quantity, change_type = 'ADJUST'), 0 " +
                "FROM " + ODS_INVENTORY + " FINAL WHERE tenant_id = " + quote(tenantId) +
                " AND toDate(create_time) = " + dateLiteral(date) +
                " GROUP BY tenant_id, toDate(create_time), warehouse_id, sku_id";
    }

    private String adsSalesInsert(String tenantId, LocalDate date) {
        return "INSERT INTO " + ADS_SALES + " (" +
                "tenant_id, stat_date, total_orders, total_gmv, total_pay, avg_order_value, " +
                "active_users, new_users, refund_amount) " +
                "SELECT tenant_id, toDate(pay_time), count(DISTINCT order_no), sum(amount), " +
                "sum(pay_amount), sum(pay_amount) / nullIf(count(DISTINCT order_no), 0), " +
                "count(DISTINCT user_id), 0, 0 " +
                "FROM " + DWD_SALES + " WHERE tenant_id = " + quote(tenantId) +
                " AND toDate(pay_time) = " + dateLiteral(date) +
                " GROUP BY tenant_id, toDate(pay_time)";
    }

    private String adsInventoryInsert(String tenantId, LocalDate date) {
        return "INSERT INTO " + ADS_INVENTORY + " (" +
                "tenant_id, stat_date, warehouse_id, sku_id, stock_qty, available_qty, reserved_qty, " +
                "turnover_days, health_status, alert_level, stock_value) " +
                "SELECT tenant_id, stat_date, warehouse_id, sku_id, closing_qty, " +
                "closing_qty - reserved_qty, reserved_qty, 0, " +
                "multiIf(closing_qty <= 0, 'CRITICAL', closing_qty < reserved_qty, 'WARNING', 'HEALTHY'), " +
                "multiIf(closing_qty <= 0, 'HIGH', closing_qty < reserved_qty, 'MEDIUM', 'NONE'), 0 " +
                "FROM " + DWS_INVENTORY + " WHERE tenant_id = " + quote(tenantId) +
                " AND stat_date = " + dateLiteral(date);
    }

    private String dateLiteral(LocalDate date) {
        return "toDate('" + date + "')";
    }

    private String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'";
    }
}
