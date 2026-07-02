package com.scmcloud.common.data.rw.health;

import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.Statement;

/**
 * MySQL replication lag checker.
 * <p>
 * Detects lag via SHOW REPLICA STATUS / SHOW SLAVE STATUS.
 * Compatible with MySQL 5.7+ and 8.0+.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class MysqlReplicationLagChecker implements ReplicationLagChecker {

    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";
    private static final String DRIVER_CLASS_LEGACY = "com.mysql.jdbc.Driver";

    @Override
    public Long checkReplicationLag(Statement statement) {
        try {
            // Try MySQL 8.0.22+ syntax first
            Long lag = tryShowReplicaStatus(statement);
            if (lag != null) {
                return lag;
            }

            // Fallback to legacy syntax
            return tryShowSlaveStatus(statement);
        } catch (Exception e) {
            log.trace("[Health] MySQL replication lag check failed: {}", e.getMessage());
        }
        return null;
    }

    /**
     * MySQL 8.0.22+ SHOW REPLICA STATUS
     */
    private Long tryShowReplicaStatus(Statement statement) {
        try {
            ResultSet rs = statement.executeQuery("SHOW REPLICA STATUS");
            return extractLagFromStatus(rs);
        } catch (Exception e) {
            log.trace("[Health] SHOW REPLICA STATUS not supported: {}", e.getMessage());
            return null;
        }
    }

    /**
     * MySQL 5.7+ SHOW SLAVE STATUS (deprecated but still available)
     */
    private Long tryShowSlaveStatus(Statement statement) {
        try {
            ResultSet rs = statement.executeQuery("SHOW SLAVE STATUS");
            return extractLagFromStatus(rs);
        } catch (Exception e) {
            log.trace("[Health] SHOW SLAVE STATUS not supported: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract lag from status result set
     */
    private Long extractLagFromStatus(ResultSet rs) throws Exception {
        if (rs != null && rs.next()) {
            // Seconds_Behind_Master / Seconds_Behind_Source field
            int lag = rs.getInt("Seconds_Behind_Master");
            if (rs.wasNull()) {
                // MySQL 8.0.22+ uses Seconds_Behind_Source
                lag = rs.getInt("Seconds_Behind_Source");
            }
            if (!rs.wasNull()) {
                return lag * 1000L; // Convert to milliseconds
            }
        }
        return null;
    }

    @Override
    public boolean supports(String driverClassName) {
        return driverClassName != null &&
                (driverClassName.toLowerCase().contains("mysql") ||
                 driverClassName.equals(DRIVER_CLASS) ||
                 driverClassName.equals(DRIVER_CLASS_LEGACY));
    }
}
