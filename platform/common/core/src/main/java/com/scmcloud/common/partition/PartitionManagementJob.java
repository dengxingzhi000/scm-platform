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
 * 分区表管理定时任务
 *
 * <p>执行时间：每月1日 01:00（cron: 0 1 1 * ?）
 *
 * <p>功能：
 * <ol>
 *   <li>预创建下月分区表（避免月末插入失败）</li>
 *   <li>清理过期分区（保留最近24个月，DETACH 归档）</li>
 * </ol>
 *
 * <p>支持的分区表：ord_order, ord_payment, ord_refund, inv_reservation,
 * inv_log, inv_batch_flow, sup_purchase_order, tenant_operation_log, payment_record
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
        new PartitionTable("ord_order"),
        new PartitionTable("ord_payment"),
        new PartitionTable("ord_refund"),
        new PartitionTable("inv_reservation"),
        new PartitionTable("inv_log"),
        new PartitionTable("inv_batch_flow"),
        new PartitionTable("sup_purchase_order"),
        new PartitionTable("tenant_operation_log"),
        new PartitionTable("payment_record")
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
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

        for (PartitionTable table : PARTITION_TABLES) {
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

    private record PartitionTable(String tableName) {
    }
}