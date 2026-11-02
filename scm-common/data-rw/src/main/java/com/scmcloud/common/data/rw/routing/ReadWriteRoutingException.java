package com.scmcloud.common.data.rw.routing;

/**
 * Raised when a read cannot be routed to a slave and the current scope forbids
 * falling back to master ({@code @Slave(fallbackToMaster = false)}).
 *
 * @author Deng
 * @since 2025-12-16
 */
public class ReadWriteRoutingException extends IllegalStateException {

    public ReadWriteRoutingException(String message) {
        super(message);
    }
}
