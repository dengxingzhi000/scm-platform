package com.scmcloud.common.partition;

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
 * Database partition management scheduled task
 *
 * Execution time: 1st day of every month at 01:00 (cron: 0 1 1 * ?)
 *
 * Functions:
 * 1. Create new partition tables for the next month (pre-create to avoid month-end insertion failures)
 * 2. Clean up expired partitions (retain last 24 months, DETACH and archive beyond that)
 * 3. Supported partition tables:
 *    - ord_order (order table, partitioned by order_time)
 *    - ord_payment (payment record, partitioned by payment_time)
 *    - ord_refund (refund record, partitioned by refund_time)
 *    - inv_reservation (inventory reservation, partitioned by create_time)
 *    - inv_log (inventory log, partitioned by create_time)
 *    - inv_batch_flow (batch flow, partitioned by create_time)
 *    - sup_purchase_order (purchase order, partitioned by order_time)
 *    - tenant_operation_log (tenant operation log, partitioned by create_time)
 *    - payment_record (financial payment record, partitioned by payment_time)
 *
 * XXL-Job configuration example:
 * - Executor: scm-common-executor
 * - JobHandler: partitionManagementJob
 * - Cron: 0 1 1 * ?
 * - Run mode: BEAN
 * - Blocking strategy: single machine serial
 * - Routing strategy: first
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
        new PartitionTable("ord_order", "order_time"),
        new PartitionTable("ord_payment", "payment_time"),
        new PartitionTable("ord_refund", "refund_time"),
        new PartitionTable("inv_reservation", "create_time"),
        new PartitionTable("inv_log", "create_time"),
        new PartitionTable("inv_batch_flow", "create_time"),
        new PartitionTable("sup_purchase_order", "order_time"),
        new PartitionTable("tenant_operation_log", "create_time"),
        new PartitionTable("payment_record", "payment_time")
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

            // 1. Create new partition for next month
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            int createdCount = createPartitionsForMonth(nextMonth);
            results.add(String.format("Created next month partitions: %d", createdCount));

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
        String partitionSuffix = yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));

        for (PartitionTable table : PARTITION_TABLES) {
            try {
                String partitionName = table.tableName + "_" + partitionSuffix;

                // Check if partition already exists
                String checkSql = """
                    SELECT COUNT(*) FROM pg_tables
                    WHERE schemaname = 'public' AND tablename = ?
                    """;
                Integer exists = jdbcTemplate.queryForObject(checkSql, Integer.class, partitionName);

                if (exists != null && exists > 0) {
                    log.debug("Partition {} already exists, skipping creation", partitionName);
                    continue;
                }

                // Calculate partition range
                LocalDate startDate = yearMonth.atDay(1);
                LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

                // Create partition table
                String createSql = String.format("""
                    CREATE TABLE IF NOT EXISTS %s PARTITION OF %s
                    FOR VALUES FROM ('%s') TO ('%s')
                    """,
                    partitionName,
                    table.tableName,
                    startDate,
                    endDate
                );

                jdbcTemplate.execute(createSql);
                log.info("Successfully created partition: {}", partitionName);
                count++;

            } catch (Exception e) {
                log.error("Failed to create partition: {}.{}", table.tableName, partitionSuffix, e);
            }
        }

        return count;
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
                        YearMonth partitionMonth = YearMonth.parse(suffix, DateTimeFormatter.ofPattern("yyyyMM"));

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

    /**
     * Partition table configuration
     */
    private record PartitionTable(String tableName, String partitionColumn) {
    }
}