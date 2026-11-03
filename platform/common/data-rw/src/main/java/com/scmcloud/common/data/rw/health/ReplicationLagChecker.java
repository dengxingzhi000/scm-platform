package com.scmcloud.common.data.rw.health;

import java.sql.Statement;

/**
 * Replication lag checker interface.
 * <p>
 * Supports different database replication lag check strategies.
 *
 * @author Deng
 * @since 2025-12-16
 */
public interface ReplicationLagChecker {

    /**
     * Check replication lag.
     *
     * @param statement SQL statement
     * @return lag in milliseconds, null if cannot detect
     */
    Long checkReplicationLag(Statement statement);

    /**
     * Check if this checker supports the given database.
     *
     * @param driverClassName JDBC driver class name
     * @return true if supported
     */
    boolean supports(String driverClassName);
}
