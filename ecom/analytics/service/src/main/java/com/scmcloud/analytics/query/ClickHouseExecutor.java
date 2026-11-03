package com.scmcloud.analytics.query;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.query.GenericRecord;
import com.clickhouse.client.api.query.QueryResponse;
import com.clickhouse.client.api.query.QuerySettings;
import com.clickhouse.client.api.query.Records;
import com.scmcloud.analytics.config.ClickHouseProperties;
import com.scmcloud.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Thin client-v2 wrapper used by the query engine and materialization jobs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseExecutor {

    private final Client clickHouseClient;
    private final ClickHouseProperties properties;

    /**
     * Execute a read query and return rows keyed by the requested columns.
     *
     * @param sql            generated SQL (never caller-supplied raw SQL)
     * @param columns        aliases to project, in output order
     * @param timeoutSeconds server-side and client-side timeout
     */
    public List<Map<String, Object>> query(String sql, List<String> columns, int timeoutSeconds) {
        QuerySettings settings = new QuerySettings()
                .waitEndOfQuery(true)
                .setMaxExecutionTime(timeoutSeconds);
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : properties.getQueryTimeoutSeconds();
        try (Records records = clickHouseClient.queryRecords(sql, Map.of(), settings)
                .get(timeout + 5L, TimeUnit.SECONDS)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (GenericRecord record : records) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String column : columns) {
                    row.put(column, record.getObject(column));
                }
                rows.add(row);
            }
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("ClickHouse query failed: {}", ex.getMessage());
            log.debug("Failed SQL: {}", sql);
            throw new BusinessException("Analytics query execution failed");
        }
    }

    /**
     * Execute a DDL / DML statement (used by materialization and quality jobs).
     *
     * <p>{@code mutations_sync=2} makes {@code DELETE FROM} wait for the
     * mutation to finish, which keeps materialization jobs idempotent.</p>
     */
    public void execute(String sql, int timeoutSeconds) {
        QuerySettings settings = new QuerySettings()
                .waitEndOfQuery(true)
                .setOption("mutations_sync", 2)
                .setMaxExecutionTime(timeoutSeconds);
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : properties.getQueryTimeoutSeconds();
        try (QueryResponse ignored = clickHouseClient.query(sql, Map.of(), settings)
                .get(timeout + 5L, TimeUnit.SECONDS)) {
            log.debug("ClickHouse statement executed");
        } catch (Exception ex) {
            log.warn("ClickHouse statement failed: {}", ex.getMessage());
            log.debug("Failed SQL: {}", sql);
            throw new BusinessException("Analytics statement execution failed");
        }
    }
}
