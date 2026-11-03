package com.scmcloud.common.data.rw.health;

import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * PostgreSQL replication lag checker.
 * <p>
 * Detects lag via pg_last_wal_receive_lsn() and pg_last_wal_replay_lsn().
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class PostgresqlReplicationLagChecker implements ReplicationLagChecker {

    @Override
    public Long checkReplicationLag(Statement statement) {
        try {
            ResultSet rs = statement.executeQuery("""
                    SELECT CASE
                        WHEN pg_last_wal_receive_lsn() = pg_last_wal_replay_lsn() THEN 0
                        ELSE EXTRACT(EPOCH FROM now() - pg_last_xact_replay_timestamp())::bigint * 1000
                    END AS lag_ms
                    """);

            if (rs.next()) {
                return rs.getLong("lag_ms");
            }
        } catch (Exception e) {
            log.trace("[Health] PostgreSQL replication lag check failed: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public boolean supports(String driverClassName) {
        return driverClassName != null && driverClassName.toLowerCase().contains("postgresql");
    }
}
