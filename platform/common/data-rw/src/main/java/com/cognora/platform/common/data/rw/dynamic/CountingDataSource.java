package com.cognora.platform.common.data.rw.dynamic;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Wraps a slave datasource and counts the connections currently held by callers.
 * <p>
 * The count is incremented when a connection is handed out and decremented when the
 * returned connection is closed (including the exception paths: a failed
 * {@code getConnection()} decrements immediately, and {@code close()} decrements
 * exactly once even if it is called twice or throws).
 * <p>
 * It is the data source behind {@code LEAST_CONNECTIONS}: the routing datasource
 * refreshes {@code SlaveInfo.activeConnections} from this counter before selecting.
 * Other load balance strategies ignore the count.
 *
 * @author Deng
 * @since 2025-12-16
 */
public class CountingDataSource implements DataSource {

    private final DataSource delegate;
    private final AtomicInteger activeConnections = new AtomicInteger();

    public CountingDataSource(DataSource delegate) {
        this.delegate = delegate;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public DataSource getDelegate() {
        return delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        activeConnections.incrementAndGet();
        try {
            return wrap(delegate.getConnection());
        } catch (SQLException | RuntimeException e) {
            activeConnections.decrementAndGet();
            throw e;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        activeConnections.incrementAndGet();
        try {
            return wrap(delegate.getConnection(username, password));
        } catch (SQLException | RuntimeException e) {
            activeConnections.decrementAndGet();
            throw e;
        }
    }

    private Connection wrap(Connection connection) {
        AtomicBoolean closed = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    try {
                        return method.invoke(connection, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    } finally {
                        if ("close".equals(method.getName()) && closed.compareAndSet(false, true)) {
                            activeConnections.decrementAndGet();
                        }
                    }
                });
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
