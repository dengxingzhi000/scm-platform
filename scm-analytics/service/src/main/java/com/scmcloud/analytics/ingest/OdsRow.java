package com.scmcloud.analytics.ingest;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

/**
 * Single ODS row destined for ClickHouse.
 * Holds the target table name (e.g. {@code ods_order}) and a column-&gt;value map.
 */
@Getter
@AllArgsConstructor
public class OdsRow {

    private final String table;
    private final Map<String, Object> fields;
}
