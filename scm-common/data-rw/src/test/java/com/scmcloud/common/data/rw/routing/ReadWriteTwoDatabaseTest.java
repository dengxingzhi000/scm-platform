package com.scmcloud.common.data.rw.routing;

import com.scmcloud.common.data.rw.config.ReadWriteProperties;
import com.scmcloud.common.data.rw.loadbalance.SlaveLoadBalancer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real two-PostgreSQL verification.
 * <p>
 * <b>Disabled by default.</b> Run it with Docker available:
 * <pre>
 *   mvn test -pl ../scm-common/data-rw -Drw.it=true -Dtest=ReadWriteTwoDatabaseTest
 * </pre>
 * The test is also skipped automatically when no Docker daemon is available
 * ({@code @Testcontainers(disabledWithoutDocker = true)}).
 * <p>
 * It verifies: writes land on master, {@code @Slave} reads land on the replica,
 * the read-after-write window returns to master, and a stopped replica degrades
 * to master instead of failing.
 */
@EnabledIfSystemProperty(named = "rw.it", matches = "true")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("真实双 PostgreSQL 读写分离验证")
class ReadWriteTwoDatabaseTest {

    @Container
    static final PostgreSQLContainer MASTER =
            new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("rw_it");

    @Container
    static final PostgreSQLContainer SLAVE =
            new PostgreSQLContainer("postgres:16-alpine").withDatabaseName("rw_it");

    private HikariDataSource masterPool;
    private HikariDataSource slavePool;
    private ReadWriteRoutingDataSource routing;

    @BeforeEach
    void setUp() throws Exception {
        masterPool = pool(MASTER);
        slavePool = pool(SLAVE);

        execute(masterPool, "CREATE TABLE IF NOT EXISTS rw_probe(id text primary key)");
        execute(slavePool, "CREATE TABLE IF NOT EXISTS rw_probe(id text primary key)");
        execute(masterPool, "DELETE FROM rw_probe");
        execute(slavePool, "DELETE FROM rw_probe");

        ReadWriteProperties properties = new ReadWriteProperties();
        properties.setValidationTimeoutSeconds(2);

        routing = new ReadWriteRoutingDataSource(
                "it", masterPool, Map.of("slave1", slavePool), properties,
                SlaveLoadBalancer.create(ReadWriteProperties.LoadBalanceType.ROUND_ROBIN), null);
        routing.setSlaveInfos(List.of(new SlaveLoadBalancer.SlaveInfo("slave1", 1, 0, true)));
        routing.afterPropertiesSet();
        ReadWriteRoutingContext.clear();
    }

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
        if (masterPool != null) {
            masterPool.close();
        }
        if (slavePool != null) {
            slavePool.close();
        }
    }

    @Test
    @DisplayName("写落主库、@Slave 读落从库、写后读回主库、从库停机降级主库")
    void shouldSplitReadsWritesAndDegradeOnSlaveOutage() throws Exception {
        // 1. write goes to master through the routing datasource
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        try (Connection connection = routing.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("INSERT INTO rw_probe(id) VALUES (?)")) {
            statement.setString(1, "master-row");
            statement.executeUpdate();
        } finally {
            ReadWriteRoutingContext.pop();
        }

        // a row that only exists on the replica proves reads are not going to master
        execute(slavePool, "INSERT INTO rw_probe(id) VALUES ('slave-row')");

        // 2. @Slave read must hit the replica
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        try {
            assertEquals(List.of("slave-row"), selectIds(routing));
            assertEquals("slave1", ReadWriteRoutingContext.currentRoute().target());

            // 3. read-after-write window forces the next read back to master
            ReadWriteRoutingContext.markWrite();
            assertEquals(List.of("master-row"), selectIds(routing));
            assertEquals("master", ReadWriteRoutingContext.currentRoute().target(), 
                    "写后读窗口内必须回到主库");
            assertFalse(ReadWriteRoutingContext.currentRoute().fallback(),
                    "这是写后读一致性覆盖，不是从库回退");
        } finally {
            ReadWriteRoutingContext.pop();
        }

        // 4. replica outage must degrade to master instead of failing
        SLAVE.stop();
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        try {
            assertEquals(List.of("master-row"), selectIds(routing));
            assertEquals("master", ReadWriteRoutingContext.currentRoute().target());
            assertTrue(ReadWriteRoutingContext.currentRoute().fallback());
        } finally {
            ReadWriteRoutingContext.pop();
        }
    }

    private HikariDataSource pool(PostgreSQLContainer container) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(container.getJdbcUrl());
        config.setUsername(container.getUsername());
        config.setPassword(container.getPassword());
        config.setMaximumPoolSize(2);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(3_000);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private void execute(DataSource dataSource, String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private List<String> selectIds(DataSource dataSource) throws Exception {
        List<String> ids = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT id FROM rw_probe ORDER BY id")) {
            while (resultSet.next()) {
                ids.add(resultSet.getString(1));
            }
        }
        return ids;
    }
}
