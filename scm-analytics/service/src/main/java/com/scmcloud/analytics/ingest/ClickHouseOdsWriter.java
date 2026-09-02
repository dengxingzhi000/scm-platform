package com.scmcloud.analytics.ingest;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.insert.InsertResponse;
import com.clickhouse.data.ClickHouseFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.analytics.config.ClickHouseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Writes ODS rows to ClickHouse using the client-v2 API.
 * <p>
 * Inserts use the JSONEachRow wire format: each row is serialized as a one-line
 * JSON object and streamed to the server. Multiple rows in one call share a
 * single round-trip and atomic server-side transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClickHouseOdsWriter {

    private final Client clickHouseClient;
    private final ClickHouseProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Insert a batch of rows into a fully-qualified ClickHouse table.
     *
     * @param tableName short table name (e.g. {@code ods_order}) — the configured
     *                  {@code clickhouse.database} is prepended automatically.
     * @param rows       the rows to insert
     * @return number of rows the call attempted to insert (best-effort echo)
     * @throws Exception on client-side serialization or ClickHouse response failure
     */
    public int insertBatch(String tableName, List<Map<String, Object>> rows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        String fqTable = properties.getDatabase() + "." + tableName;
        try {
            byte[] body = serializeJsonEachRow(rows);
            ByteArrayInputStream stream = new ByteArrayInputStream(body);
            InsertResponse response = clickHouseClient
                    .insert(fqTable, stream, ClickHouseFormat.JSONEachRow)
                    .get(30, TimeUnit.SECONDS);
            log.debug("Inserted {} rows into {}: readRows={}, writtenRows={}",
                    rows.size(), fqTable, response.getReadRows(), response.getWrittenRows());
            return rows.size();
        } catch (Exception ex) {
            log.warn("ClickHouse insert failed for {} rows into {}: {}",
                    rows.size(), fqTable, ex.getMessage());
            throw ex;
        }
    }

    private byte[] serializeJsonEachRow(List<Map<String, Object>> rows) throws Exception {
        StringBuilder sb = new StringBuilder(rows.size() * 128);
        for (Map<String, Object> row : rows) {
            sb.append(objectMapper.writeValueAsString(row)).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
