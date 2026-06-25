package com.scmcloud.common.data.rw.sql;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SQL Type Parser Test")
class SqlTypeParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM users",
            "SELECT id, name FROM users WHERE id = 1",
            "SHOW TABLES",
            "DESCRIBE users",
            "EXPLAIN SELECT * FROM users"
    })
    @DisplayName("Should parse read operations")
    void shouldParseReadOperations(String sql) {
        assertEquals(SqlTypeParser.SqlType.READ, SqlTypeParser.parse(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO users (name) VALUES ('test')",
            "UPDATE users SET name = 'test' WHERE id = 1",
            "DELETE FROM users WHERE id = 1",
            "CREATE TABLE test (id INT)",
            "ALTER TABLE users ADD COLUMN age INT",
            "DROP TABLE test",
            "TRUNCATE TABLE users"
    })
    @DisplayName("Should parse write operations")
    void shouldParseWriteOperations(String sql) {
        assertEquals(SqlTypeParser.SqlType.WRITE, SqlTypeParser.parse(sql));
    }

    @Test
    @DisplayName("Should parse SELECT FOR UPDATE")
    void shouldParseSelectForUpdate() {
        String sql = "SELECT * FROM users WHERE id = 1 FOR UPDATE";
        assertEquals(SqlTypeParser.SqlType.WRITE, SqlTypeParser.parse(sql));
    }

    @Test
    @DisplayName("Should parse LOCK IN SHARE MODE")
    void shouldParseLockInShareMode() {
        String sql = "SELECT * FROM users WHERE id = 1 LOCK IN SHARE MODE";
        assertEquals(SqlTypeParser.SqlType.WRITE, SqlTypeParser.parse(sql));
    }

    @Test
    @DisplayName("Should handle null and empty SQL")
    void shouldHandleNullSql() {
        assertEquals(SqlTypeParser.SqlType.UNKNOWN, SqlTypeParser.parse(null));
        assertEquals(SqlTypeParser.SqlType.UNKNOWN, SqlTypeParser.parse(""));
        assertEquals(SqlTypeParser.SqlType.UNKNOWN, SqlTypeParser.parse("   "));
    }

    @Test
    @DisplayName("Should parse MASTER hint")
    void shouldParseMasterHint() {
        String sql = "/*MASTER*/ SELECT * FROM users";
        SqlTypeParser.RoutingHint hint = SqlTypeParser.parseHint(sql);

        assertEquals(SqlTypeParser.RoutingHint.HintType.MASTER, hint.type());
        assertNull(hint.slaveName());
    }

    @Test
    @DisplayName("Should parse SLAVE hint")
    void shouldParseSlaveHint() {
        String sql = "/*SLAVE*/ SELECT * FROM users";
        SqlTypeParser.RoutingHint hint = SqlTypeParser.parseHint(sql);

        assertEquals(SqlTypeParser.RoutingHint.HintType.SLAVE, hint.type());
        assertNull(hint.slaveName());
    }

    @Test
    @DisplayName("Should parse SLAVE hint with name")
    void shouldParseSlaveHintWithName() {
        String sql = "/*SLAVE(slave1)*/ SELECT * FROM users";
        SqlTypeParser.RoutingHint hint = SqlTypeParser.parseHint(sql);

        assertEquals(SqlTypeParser.RoutingHint.HintType.SLAVE, hint.type());
        assertEquals("slave1", hint.slaveName());
    }

    @Test
    @DisplayName("Should remove hint comments")
    void shouldRemoveHints() {
        String sql = "/*MASTER*/ SELECT * FROM users";
        assertEquals("SELECT * FROM users", SqlTypeParser.removeHint(sql));

        sql = "/*SLAVE*/ SELECT * FROM users";
        assertEquals("SELECT * FROM users", SqlTypeParser.removeHint(sql));

        sql = "/*SLAVE(slave1)*/ SELECT * FROM users";
        assertEquals("SELECT * FROM users", SqlTypeParser.removeHint(sql));
    }

    @Test
    @DisplayName("Should identify transaction statements")
    void shouldIdentifyTransactionStatements() {
        assertTrue(SqlTypeParser.isTransactionStatement("BEGIN"));
        assertTrue(SqlTypeParser.isTransactionStatement("COMMIT"));
        assertTrue(SqlTypeParser.isTransactionStatement("ROLLBACK"));
        assertTrue(SqlTypeParser.isTransactionStatement("SAVEPOINT sp1"));
        assertTrue(SqlTypeParser.isTransactionStatement("SET AUTOCOMMIT = 0"));

        assertFalse(SqlTypeParser.isTransactionStatement("SELECT * FROM users"));
        assertFalse(SqlTypeParser.isTransactionStatement(null));
    }
}
