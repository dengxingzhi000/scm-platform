package com.cognora.platform.common.partition;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 分区表管理定时任务
 *
 * <p>执行时间：建议每日 02:00（cron: 0 0 2 * * ?），由 XXL-Job 调度。
 *
 * <p>功能：
 * <ol>
 *   <li>幂等补建「当月 + 未来 3 个月」分区（避免月末/跨月插入失败）</li>
 *   <li>清理过期分区（保留最近24个月，DETACH 归档）</li>
 * </ol>
 *
 * <p>只处理当前数据源中真实存在的父表：同一 handler 在不同服务的执行器上
 * 运行时，会各自管理本库的分区（例如 scm-order 管 db_order，
 * scm-audit 管 db_audit.sys_audit_log）。</p>
 *
 * <p>支持的分区表：ord_order, ord_payment, ord_refund, inv_reservation,
 * inv_log, inv_batch_flow, sup_purchase_order, tenant_operation_log, payment_record
 * （<code>&lt;table&gt;_yyyyMM</code>），以及 db_audit.sys_audit_log
 * （<code>&lt;table&gt;_yyyy_MM</code>，命名沿用 005_db_audit.sql）。
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartitionManagementJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Partition tables to manage
     */
    private static final List<PartitionTable> PARTITION_TABLES = List.of(
        new PartitionTable("ord_order", "yyyyMM"),
        new PartitionTable("ord_payment", "yyyyMM"),
        new PartitionTable("ord_refund", "yyyyMM"),
        new PartitionTable("inv_reservation", "yyyyMM"),
        new PartitionTable("inv_log", "yyyyMM"),
        new PartitionTable("inv_batch_flow", "yyyyMM"),
        new PartitionTable("sup_purchase_order", "yyyyMM"),
        new PartitionTable("tenant_operation_log", "yyyyMM"),
        new PartitionTable("payment_record", "yyyyMM"),
        new PartitionTable("sys_audit_log", "yyyy_MM")
    );

    /**
     * Retention period for partitions (24 months = 2 years)
     */
    private static final int RETENTION_MONTHS = 24;

    /**
     * Execute partition management task
     */
    @XxlJob("partitionManagementJob")
    public void execute() {
        long startTime = System.currentTimeMillis();
        List<String> results = new ArrayList<>();

        try {
            log.info("Starting partition management task");

            // 1. 幂等补建当月 + 未来 3 个月分区
            int createdCount = 0;
            YearMonth currentMonth = YearMonth.now();
            for (int offset = 0; offset <= 3; offset++) {
                createdCount += createPartitionsForMonth(currentMonth.plusMonths(offset));
            }
            results.add(String.format("Created partitions for %s..%s: %d",
                currentMonth, currentMonth.plusMonths(3), createdCount));

            // 2. Clean up expired partitions
            YearMonth cutoffMonth = YearMonth.now().minusMonths(RETENTION_MONTHS);
            int detachedCount = detachExpiredPartitions(cutoffMonth);
            results.add(String.format("Archived expired partitions: %d", detachedCount));

            long duration = System.currentTimeMillis() - startTime;
            String successMsg = String.format("Partition management completed [%s], duration: %d ms",
                String.join(", ", results), duration);

            log.info(successMsg);
            XxlJobHelper.handleSuccess(successMsg);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMsg = String.format("Partition management failed, duration: %d ms", duration);
            log.error(errorMsg, e);
            XxlJobHelper.handleFail(errorMsg + ": " + e.getMessage());
        }
    }

    /**
     * Create partitions for the specified month
     */
    private int createPartitionsForMonth(YearMonth yearMonth) {
        int count = 0;
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

        for (PartitionTable table : PARTITION_TABLES) {
            if (!parentTableExists(table.tableName())) {
                log.debug("Skip partition creation, parent table not in this database: {}", table.tableName());
                continue;
            }
            String partitionSuffix = yearMonth.format(DateTimeFormatter.ofPattern(table.suffixPattern()));
            try {
                String partitionName = table.tableName + "_" + partitionSuffix;
                String createSql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF %s FOR VALUES FROM ('%s') TO ('%s')",
                    partitionName, table.tableName, startDate, endDate
                );

                jdbcTemplate.execute(createSql);
                log.info("Created partition: {}", partitionName);
                count++;
            } catch (Exception e) {
                log.error("Failed to create partition: {}.{}", table.tableName, partitionSuffix, e);
            }
        }

        return count;
    }

    /**
     * 当前数据源是否包含该父表（分区表在 information_schema 中仍是普通表）。
     */
    private boolean parentTableExists(String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + tableName);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Detach expired partitions (archive)
     */
    private int detachExpiredPartitions(YearMonth cutoffMonth) {
        int count = 0;

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                // Query all partitions
                String querySql = """
                    SELECT tablename FROM pg_tables
                    WHERE schemaname = 'public'
                      AND tablename LIKE ?
                    ORDER BY tablename
                    """;
                List<String> partitions = jdbcTemplate.queryForList(
                    querySql,
                    String.class,
                    table.tableName + "_%"
                );

                for (String partition : partitions) {
                    // Extract year-month from partition name (e.g. ord_order_202401 -> 202401)
                    String suffix = partition.substring(table.tableName.length() + 1);
                    try {
                        YearMonth partitionMonth = YearMonth.parse(
                            suffix, DateTimeFormatter.ofPattern(table.suffixPattern()));

                        if (partitionMonth.isBefore(cutoffMonth)) {
                            // DETACH partition (do not delete data, only detach from parent table)
                            String detachSql = String.format(
                                "ALTER TABLE %s DETACH PARTITION %s",
                                table.tableName,
                                partition
                            );
                            jdbcTemplate.execute(detachSql);
                            log.info("Successfully archived partition: {}", partition);
                            count++;
                        }
                    } catch (Exception e) {
                        log.warn("Unable to parse partition suffix: {}", partition);
                    }
                }

            } catch (Exception e) {
                log.error("Failed to archive partitions: {}", table.tableName, e);
            }
        }

        return count;
    }

    private record PartitionTable(String tableName, String suffixPattern) {
    }
}