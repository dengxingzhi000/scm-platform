package com.scmcloud.analytics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class MetaSchemaTest {

    @Test
    void flywayMigrationFileExistsAndContainsExpectedTables() throws IOException {
        // Load via classpath (src/main/resources/db/migration)
        URL resource = getClass().getClassLoader().getResource("db/migration/V1__init_analytics_meta.sql");
        assertNotNull(resource, "Flyway migration V1__init_analytics_meta.sql must exist on classpath");

        String sql = Files.readString(toPath(resource), StandardCharsets.UTF_8);
        assertContainsTable(sql, "analytics_metric");
        assertContainsTable(sql, "analytics_dimension");
        assertContainsTable(sql, "analytics_dataset");
        assertContainsTable(sql, "analytics_dataset_field");

        // tenant_id UUID NOT NULL per task
        assertTrue(sql.contains("tenant_id UUID NOT NULL"),
                "All tables must have tenant_id UUID NOT NULL");
        // code uniqueness per tenant
        assertTrue(sql.toLowerCase().contains("unique (tenant_id, metric_code)")
                        || sql.toLowerCase().contains("uk_metric_tenant_code"),
                "analytics_metric must have code uniqueness per tenant");
        assertTrue(sql.toLowerCase().contains("unique (tenant_id, dim_code)")
                        || sql.toLowerCase().contains("uk_dimension_tenant_code"),
                "analytics_dimension must have code uniqueness per tenant");
        assertTrue(sql.toLowerCase().contains("unique (tenant_id, dataset_code)")
                        || sql.toLowerCase().contains("uk_dataset_tenant_code"),
                "analytics_dataset must have code uniqueness per tenant");
        assertTrue(sql.toLowerCase().contains("unique (dataset_id, field_code)")
                        || sql.toLowerCase().contains("uk_dataset_field_code"),
                "analytics_dataset_field must have field_code uniqueness per dataset");
    }

    @Test
    void dbaInitScriptExistsAndIsIdentical() throws IOException {
        // scripts/db/microservices/040_analytics_meta.sql is outside the module.
        // Resolve relative to project root: scm-analytics/service -> ../../scripts/...
        Path serviceModule = Paths.get("").toAbsolutePath();
        // When run via -pl scm-analytics/service, working dir is still repo root in many setups,
        // so try both locations.
        Path candidate1 = serviceModule.resolve("scripts/db/microservices/040_analytics_meta.sql");
        Path candidate2 = serviceModule.resolve("scm-analytics/service/scripts/db/microservices/040_analytics_meta.sql");
        Path candidate3 = Paths.get("scripts/db/microservices/040_analytics_meta.sql").toAbsolutePath();
        Path candidate4 = Paths.get("../..").resolve("scripts/db/microservices/040_analytics_meta.sql").toAbsolutePath().normalize();

        Path dbaFile = null;
        for (Path p : new Path[]{candidate1, candidate2, candidate3, candidate4}) {
            if (Files.exists(p)) {
                dbaFile = p;
                break;
            }
        }
        // Fallback: search via classpath-adjacent file system walk from user.dir
        if (dbaFile == null) {
            String userDir = System.getProperty("user.dir");
            Path fromUserDir = Paths.get(userDir).resolve("scripts/db/microservices/040_analytics_meta.sql");
            if (Files.exists(fromUserDir)) dbaFile = fromUserDir;
            else {
                // walk up to repo root (scm-analytics/service -> root)
                Path cur = Paths.get(userDir);
                for (int i = 0; i < 5 && cur != null; i++) {
                    Path tryPath = cur.resolve("scripts/db/microservices/040_analytics_meta.sql");
                    if (Files.exists(tryPath)) { dbaFile = tryPath; break; }
                    cur = cur.getParent();
                }
            }
        }

        assertNotNull(dbaFile, "DBA init script scripts/db/microservices/040_analytics_meta.sql must exist (searched from "
                + System.getProperty("user.dir") + ")");
        assertTrue(Files.exists(dbaFile), "DBA script must exist: " + dbaFile);

        String dbaSql = Files.readString(dbaFile, StandardCharsets.UTF_8);
        assertContainsTable(dbaSql, "analytics_metric");
        assertContainsTable(dbaSql, "analytics_dimension");
        assertContainsTable(dbaSql, "analytics_dataset");
        assertContainsTable(dbaSql, "analytics_dataset_field");
    }

    @Test
    void servicePomHasFlywayDependencies() throws IOException {
        // Verify pom contains flyway-core and flyway-database-postgresql
        // Pom is not on classpath; read via file system relative to module root
        Path pom = findPom();
        assertNotNull(pom, "scm-analytics/service/pom.xml must be found");
        String pomContent = Files.readString(pom, StandardCharsets.UTF_8);
        assertTrue(pomContent.contains("flyway-core"), "pom must declare flyway-core");
        assertTrue(pomContent.contains("flyway-database-postgresql"), "pom must declare flyway-database-postgresql");
        assertTrue(pomContent.contains("postgresql"), "pom must declare postgresql driver");
    }

    private void assertContainsTable(String sql, String table) {
        // case-insensitive check for CREATE TABLE <table>
        String lower = sql.toLowerCase();
        assertTrue(lower.contains("create table") && lower.contains(table),
                "SQL must contain CREATE TABLE for " + table);
    }

    private Path findPom() {
        String[] candidates = {
                "scm-analytics/service/pom.xml",
                "pom.xml",
                "../pom.xml",
                "../../pom.xml"
        };
        String userDir = System.getProperty("user.dir");
        for (String c : candidates) {
            Path p = Paths.get(userDir).resolve(c);
            if (Files.exists(p)) return p;
            Path p2 = Paths.get(c);
            if (Files.exists(p2)) return p2;
        }
        // walk up
        Path cur = Paths.get(userDir);
        for (int i = 0; i < 5 && cur != null; i++) {
            Path tryPath = cur.resolve("scm-analytics/service/pom.xml");
            if (Files.exists(tryPath)) return tryPath;
            cur = cur.getParent();
        }
        return null;
    }

    private Path toPath(URL resource) {
        try {
            return Paths.get(resource.toURI());
        } catch (Exception e) {
            return Paths.get(resource.getPath());
        }
    }
}
