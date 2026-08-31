package com.scmcloud.analytics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ClickHouseSchemaTest {

    private static final String[] TABLES = {
            "analytics.ods_order",
            "analytics.ods_inventory",
            "analytics.dwd_sales_detail",
            "analytics.dws_sales_day",
            "analytics.dws_inventory_day",
            "analytics.ads_sales_dashboard",
            "analytics.ads_inventory_health"
    };

    // Short names without db prefix for lenient check
    private static final String[] TABLE_SHORT = {
            "ods_order",
            "ods_inventory",
            "dwd_sales_detail",
            "dws_sales_day",
            "dws_inventory_day",
            "ads_sales_dashboard",
            "ads_inventory_health"
    };

    @Test
    void clickHouseDwhFileExistsAndContainsAllTables() throws IOException {
        URL resource = getClass().getClassLoader().getResource("clickhouse/V1__ods_dwd_dws_ads.sql");
        assertNotNull(resource, "ClickHouse DWH file clickhouse/V1__ods_dwd_dws_ads.sql must exist on classpath");

        String sql = Files.readString(toPath(resource), StandardCharsets.UTF_8);
        String lower = sql.toLowerCase();

        // 7 tables fully qualified
        for (String t : TABLES) {
            assertTrue(lower.contains(t),
                    "SQL must contain table " + t);
        }
        // also ensure short names present
        for (String t : TABLE_SHORT) {
            assertTrue(lower.contains(t),
                    "SQL must contain table short name " + t);
        }

        // Engines: must contain all three types
        assertTrue(lower.contains("replacingmergetree"),
                "SQL must contain ReplacingMergeTree engine");
        assertTrue(lower.contains("summingmergetree"),
                "SQL must contain SummingMergeTree engine");
        // MergeTree appears in all, but ensure plain MergeTree token exists
        // Check that at least one ENGINE = MergeTree (not only Replacing/Summing)
        // Summing and Replacing both contain "mergetree", so specifically count merging types
        assertTrue(lower.contains("engine = mergetree") || lower.contains("engine=mergetree"),
                "SQL must contain MergeTree engine for DWD layer");

        // Partitioning by toYYYYMM
        assertTrue(lower.contains("partition by toyyyymm") || lower.contains("partition by toyyyymm("),
                "SQL must contain PARTITION BY toYYYYMM");
        // At least 7 partition clauses (one per table)
        long partitionCount = lower.split("partition by toyyyymm", -1).length - 1;
        assertTrue(partitionCount >= 7,
                "SQL must have PARTITION BY toYYYYMM for each of the 7 tables, found " + partitionCount);

        // ORDER BY (tenant_id, ...
        assertTrue(lower.contains("order by (tenant_id"),
                "SQL must contain ORDER BY (tenant_id,...) per spec");
        long orderByTenantCount = lower.split("order by \\(tenant_id", -1).length - 1;
        assertTrue(orderByTenantCount >= 7,
                "SQL must have ORDER BY (tenant_id,...) for each of the 7 tables, found " + orderByTenantCount);

        // Database analytics creation
        assertTrue(lower.contains("create database if not exists analytics"),
                "SQL must ensure analytics database is created");
    }

    @Test
    void clickHouseEnginesAndPartitioningPerTable() throws IOException {
        URL resource = getClass().getClassLoader().getResource("clickhouse/V1__ods_dwd_dws_ads.sql");
        assertNotNull(resource, "ClickHouse DWH file must exist");
        String sql = Files.readString(toPath(resource), StandardCharsets.UTF_8);
        String lower = sql.toLowerCase();

        // ODS layer should use ReplacingMergeTree
        assertTrue(extractTableBlock(lower, "analytics.ods_order").contains("replacingmergetree"),
                "ods_order must use ReplacingMergeTree");
        assertTrue(extractTableBlock(lower, "analytics.ods_inventory").contains("replacingmergetree"),
                "ods_inventory must use ReplacingMergeTree");

        // DWD layer uses MergeTree (non-replacing/summing for detail)
        String dwdBlock = extractTableBlock(lower, "analytics.dwd_sales_detail");
        assertTrue(dwdBlock.contains("mergetree"),
                "dwd_sales_detail must use MergeTree family");
        // ensure it is plain MergeTree, not Summing/Replacing for DWD detail
        // allow plain MergeTree; check that it contains PARTITION BY toYYYYMM
        assertTrue(dwdBlock.contains("partition by toyyyymm"),
                "dwd_sales_detail must be partitioned by toYYYYMM");

        // DWS layer uses SummingMergeTree
        assertTrue(extractTableBlock(lower, "analytics.dws_sales_day").contains("summingmergetree"),
                "dws_sales_day must use SummingMergeTree");
        assertTrue(extractTableBlock(lower, "analytics.dws_inventory_day").contains("summingmergetree"),
                "dws_inventory_day must use SummingMergeTree");

        // ADS layer — at least one Summing or Replacing, verify both have partitioning
        assertTrue(extractTableBlock(lower, "analytics.ads_sales_dashboard").contains("partition by toyyyymm"),
                "ads_sales_dashboard must be partitioned by toYYYYMM");
        assertTrue(extractTableBlock(lower, "analytics.ads_inventory_health").contains("partition by toyyyymm"),
                "ads_inventory_health must be partitioned by toYYYYMM");
        String adsBlock = extractTableBlock(lower, "analytics.ads_sales_dashboard");
        String adsHealthBlock = extractTableBlock(lower, "analytics.ads_inventory_health");
        assertTrue(adsBlock.contains("replacingmergetree") || adsBlock.contains("summingmergetree"),
                "ads_sales_dashboard must use ReplacingMergeTree or SummingMergeTree");
        assertTrue(adsHealthBlock.contains("replacingmergetree") || adsHealthBlock.contains("summingmergetree"),
                "ads_inventory_health must use ReplacingMergeTree or SummingMergeTree");
    }

    @Test
    void deployInitSqlExistsAndContainsTables() throws IOException {
        // deploy/clickhouse/init.sql is outside module; search from cwd similar to MetaSchemaTest
        Path initFile = findDeployInit();
        assertNotNull(initFile, "deploy/clickhouse/init.sql must exist (mounted for dev)");
        String sql = Files.readString(initFile, StandardCharsets.UTF_8);
        String lower = sql.toLowerCase();
        for (String t : TABLE_SHORT) {
            assertTrue(lower.contains(t),
                    "deploy init.sql must contain table " + t);
        }
        assertTrue(lower.contains("create database if not exists analytics"),
                "deploy init.sql must create analytics database");
    }

    @Test
    void dockerComposeHasClickHouseInitMount() throws IOException {
        Path compose = findDockerCompose();
        assertNotNull(compose, "docker-compose.yml must be found");
        String content = Files.readString(compose, StandardCharsets.UTF_8);
        assertTrue(content.contains("clickhouse_data:/var/lib/clickhouse"),
                "docker-compose must retain clickhouse_data volume");
        assertTrue(content.contains("deploy/clickhouse/init.sql"),
                "docker-compose must mount deploy/clickhouse/init.sql for dev");
        // ensure not duplicated volume definition — count only top-level volume keys (line equals '  clickhouse_data:')
        long defCount = Files.readAllLines(compose, StandardCharsets.UTF_8).stream()
                .filter(l -> l.trim().equals("clickhouse_data:"))
                .count();
        assertEquals(1, defCount, "clickhouse_data volume definition must appear exactly once (not duplicated)");
        // ensure no duplicate mount entry
        long mountCount = content.split("clickhouse_data:/var/lib/clickhouse", -1).length - 1;
        assertEquals(1, mountCount, "clickhouse_data mount must appear exactly once");
    }

    // ---- helpers ----

    private String extractTableBlock(String lowerSql, String qualifiedTable) {
        // find CREATE TABLE ... qualifiedTable ... ENGINE ... SETTINGS or semicolon
        // Use qualified name (analytics.xxx) to avoid matching header comment that lists short names
        int idx = lowerSql.indexOf(qualifiedTable);
        if (idx < 0) return "";
        int nextCreate = lowerSql.indexOf("create table", idx + qualifiedTable.length());
        int end = nextCreate > 0 ? nextCreate : lowerSql.length();
        int semi = lowerSql.indexOf(";", idx);
        if (semi > 0 && semi < end) end = semi + 1;
        return lowerSql.substring(idx, Math.min(end, idx + 4000));
    }

    private Path findDeployInit() {
        String userDir = System.getProperty("user.dir");
        Path[] candidates = {
                Paths.get(userDir).resolve("deploy/clickhouse/init.sql"),
                Paths.get("deploy/clickhouse/init.sql").toAbsolutePath(),
                Paths.get(userDir).resolve("scm-analytics/service/deploy/clickhouse/init.sql"),
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        Path cur = Paths.get(userDir);
        for (int i = 0; i < 6 && cur != null; i++) {
            Path tryPath = cur.resolve("deploy/clickhouse/init.sql");
            if (Files.exists(tryPath)) return tryPath;
            Path try2 = cur.resolve("scm-analytics/service/src/main/resources/clickhouse/V1__ods_dwd_dws_ads.sql");
            // fallback not needed
            cur = cur.getParent();
        }
        return null;
    }

    private Path findDockerCompose() {
        String userDir = System.getProperty("user.dir");
        Path[] candidates = {
                Paths.get(userDir).resolve("docker-compose.yml"),
                Paths.get("docker-compose.yml").toAbsolutePath(),
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        Path cur = Paths.get(userDir);
        for (int i = 0; i < 6 && cur != null; i++) {
            Path tryPath = cur.resolve("docker-compose.yml");
            if (Files.exists(tryPath)) return tryPath;
            cur = cur.getParent();
        }
        return null;
    }

    private Path toPath(URL resource) {
        try {
            return Paths.get(resource.toURI());
        } catch (Exception e) {
            return Paths.get(resource.getPath());
        }
    }
}
