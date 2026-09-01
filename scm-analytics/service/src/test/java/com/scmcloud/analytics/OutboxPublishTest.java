package com.scmcloud.analytics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class OutboxPublishTest {

    @Test
    void outboxSqlFileExistsAndContainsExpectedTable() throws IOException {
        Path dbaFile = findOutboxSql();
        assertNotNull(dbaFile, "scripts/db/microservices/041_outbox.sql must exist (searched from " + System.getProperty("user.dir") + ")");
        assertTrue(Files.exists(dbaFile), "041_outbox.sql must exist: " + dbaFile);

        String sql = Files.readString(dbaFile, StandardCharsets.UTF_8);
        String lower = sql.toLowerCase();

        assertTrue(lower.contains("create table") && lower.contains("outbox_event"),
                "SQL must contain CREATE TABLE outbox_event");
        assertTrue(lower.contains("id") && lower.contains("varchar(36)") || lower.contains("varchar(36) primary key"),
                "outbox_event must have id VARCHAR(36) PK");
        assertTrue(lower.contains("tenant_id") && lower.contains("uuid") && lower.contains("not null"),
                "outbox_event must have tenant_id UUID NOT NULL");
        assertTrue(lower.contains("aggregate_type") && lower.contains("varchar(64)"),
                "outbox_event must have aggregate_type VARCHAR(64) NOT NULL");
        assertTrue(lower.contains("aggregate_id") && lower.contains("varchar(64)"),
                "outbox_event must have aggregate_id VARCHAR(64) NOT NULL");
        assertTrue(lower.contains("event_type") && lower.contains("varchar(64)"),
                "outbox_event must have event_type VARCHAR(64) NOT NULL");
        assertTrue(lower.contains("payload") && lower.contains("jsonb") && lower.contains("not null"),
                "outbox_event must have payload JSONB NOT NULL");
        assertTrue(lower.contains("published") && lower.contains("boolean"),
                "outbox_event must have published BOOLEAN");
        assertTrue(lower.contains("published_at"),
                "outbox_event must have published_at column");
        // partial index published=false
        assertTrue(lower.contains("where published=false") || lower.contains("where published = false"),
                "SQL must contain partial index WHERE published=false");
        assertTrue(lower.contains("idx_outbox") || lower.contains("create index"),
                "SQL must contain index ON (published, created_at)");
    }

    @Test
    void orderOutboxEntityExistsAndHasTableName() throws Exception {
        // Check via file existence (no DB) — verify source has @TableName("outbox_event")
        Path entityFile = findFile("scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OutboxEvent.java");
        assertNotNull(entityFile, "OutboxEvent.java must exist in scm-order/service");
        String content = Files.readString(entityFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("@TableName") && content.contains("outbox_event"),
                "OutboxEvent must have @TableName(\"outbox_event\")");
        assertTrue(content.contains("tenantId") || content.contains("tenant_id"),
                "OutboxEvent must have tenantId field");
        assertTrue(content.contains("aggregateType") || content.contains("aggregate_type"),
                "OutboxEvent must have aggregateType field");
        assertTrue(content.contains("payload"),
                "OutboxEvent must have payload field");
        assertTrue(content.contains("published"),
                "OutboxEvent must have published field");
    }

    @Test
    void orderOutboxMapperExistsAndHasMethods() throws IOException {
        Path mapperFile = findFile("scm-order/service/src/main/java/com/scmcloud/order/mapper/OutboxMapper.java");
        assertNotNull(mapperFile, "OutboxMapper.java must exist in scm-order/service");
        String content = Files.readString(mapperFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("extends BaseMapper<OutboxEvent>") || content.contains("extends BaseMapper"),
                "OutboxMapper must extend BaseMapper<OutboxEvent>");
        assertTrue(content.contains("findUnpublished"),
                "OutboxMapper must have findUnpublished(limit) method");
        assertTrue(content.contains("markPublished"),
                "OutboxMapper must have markPublished(id) method");
        assertTrue(content.contains("countByAggregate"),
                "OutboxMapper must have countByAggregate method");
        assertTrue(content.contains("FOR UPDATE SKIP LOCKED") || content.contains("published = false"),
                "OutboxMapper polling must filter published=false");
    }

    @Test
    void inventoryOutboxTemplateExists() throws IOException {
        // Template copy — inventory should also have outbox
        Path invEntity = findFile("scm-inventory/service/src/main/java/com/scmcloud/inventory/domain/entity/OutboxEvent.java");
        Path invMapper = findFile("scm-inventory/service/src/main/java/com/scmcloud/inventory/mapper/OutboxMapper.java");
        // inventory copy is optional per spec "or note as template", so at least one must exist or analytics relay documents it
        if (invEntity == null && invMapper == null) {
            // fallback: ensure analytics relay documents inventory template in comments
            Path relay = findFile("scm-analytics/service/src/main/java/com/scmcloud/analytics/ingest/OutboxRelayJob.java");
            assertNotNull(relay, "OutboxRelayJob must exist");
            String relayContent = Files.readString(relay, StandardCharsets.UTF_8);
            assertTrue(relayContent.toLowerCase().contains("inventory") || relayContent.toLowerCase().contains("template"),
                    "If inventory copy missing, relay must mention inventory template");
        } else {
            if (invEntity != null) {
                String c = Files.readString(invEntity, StandardCharsets.UTF_8);
                assertTrue(c.contains("outbox_event"), "Inventory OutboxEvent must map to outbox_event");
            }
            if (invMapper != null) {
                String c = Files.readString(invMapper, StandardCharsets.UTF_8);
                assertTrue(c.contains("findUnpublished"), "Inventory OutboxMapper must have findUnpublished");
            }
        }
    }

    @Test
    void outboxRelayJobExistsAndHasScheduledKafka() throws IOException {
        Path relayFile = findFile("scm-analytics/service/src/main/java/com/scmcloud/analytics/ingest/OutboxRelayJob.java");
        assertNotNull(relayFile, "OutboxRelayJob.java must exist in scm-analytics/service");
        String content = Files.readString(relayFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("@Component") || content.contains("@Service"),
                "OutboxRelayJob must be @Component");
        assertTrue(content.contains("@Scheduled") && content.contains("fixedDelay"),
                "OutboxRelayJob must have @Scheduled(fixedDelay=5000) relay()");
        assertTrue(content.contains("fixedDelay=5000") || content.contains("fixedDelay = 5000"),
                "OutboxRelayJob fixedDelay must be 5000");
        assertTrue(content.toLowerCase().contains("kafkatemplate") && content.contains("send("),
                "OutboxRelayJob must send via kafkaTemplate.send");
        assertTrue(content.contains("scm.") || content.contains("\"scm.\"+"),
                "OutboxRelayJob must publish to topic \"scm.\"+aggregateType");
        assertTrue(content.contains("markPublished") || content.contains("published"),
                "OutboxRelayJob must mark published after send");
        // DistributedLock or comment
        assertTrue(content.contains("DistributedLock") || content.contains("distributed lock") || content.toLowerCase().contains("skip locked"),
                "OutboxRelayJob must mention @DistributedLock or SKIP LOCKED for multi-instance safety");
    }

    @Test
    void orderCommandServiceWritesOutboxInSameTx() throws IOException {
        // Check that order command service demonstrates same-TX outbox insert with @Master @Transactional
        Path svc = findFile("scm-order/service/src/main/java/com/scmcloud/order/service/command/OrdOrderCommandService.java");
        if (svc == null) {
            svc = findFile("scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java");
        }
        assertNotNull(svc, "Order command service must exist");
        String content = Files.readString(svc, StandardCharsets.UTF_8);
        assertTrue(content.contains("OutboxMapper") || content.contains("outboxMapper"),
                "Order service must inject OutboxMapper for same-TX outbox insert");
        assertTrue(content.contains("OutboxEvent"),
                "Order service must create OutboxEvent in same transaction");
        assertTrue(content.contains("@Master") && content.contains("@Transactional"),
                "Order service outbox TX must have @Master @Transactional");
        assertTrue(content.contains("TenantContextHolder") || content.contains("tenantId"),
                "Order service outbox must carry tenant_id via TenantContextHolder");
    }

    @Test
    void outboxAtomicityIsMandatoryAndTenantFailFast() throws IOException {
        // Verifies reviewer fix 1 & 3: outbox insert must be mandatory (no null-guard) and tenant fail-fast
        Path cmdSvc = findFile("scm-order/service/src/main/java/com/scmcloud/order/service/command/OrdOrderCommandService.java");
        Path implSvc = findFile("scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrdOrderServiceImpl.java");
        Path invSvc = findFile("scm-inventory/service/src/main/java/com/scmcloud/inventory/service/command/InvInventoryCommandService.java");
        for (Path svc : new Path[]{cmdSvc, implSvc, invSvc}) {
            assertNotNull(svc, "Service file must exist: " + svc);
            String content = Files.readString(svc, StandardCharsets.UTF_8);
            assertFalse(content.contains("if (outboxMapper != null)"),
                    "Outbox insert must be mandatory — no `if (outboxMapper != null)` guard allowed: " + svc);
            // Fallback tenant fabrication must not exist: check for catch+randomUUID pattern
            assertFalse(content.contains("catch (Exception") && content.contains("randomUUID"),
                    "Tenant must be fail-fast via TenantContextHolder.getRequiredTenantId(), no UUID.randomUUID() fallback: " + svc);
            assertFalse(content.contains("fallback for non-tenant") || content.contains("fallback tenantId"),
                    "No tenant fallback comment allowed: " + svc);
            assertTrue(content.contains("TenantContextHolder.getRequiredTenantId()"),
                    "Service must use TenantContextHolder.getRequiredTenantId() fail-fast: " + svc);
        }
        // Check @Master on OrdOrderServiceImpl.createOrder and active @DistributedLockAnnotation on relay
        assertNotNull(implSvc, "OrdOrderServiceImpl must exist");
        String implContent = Files.readString(implSvc, StandardCharsets.UTF_8);
        assertTrue(implContent.contains("@Master") && implContent.contains("createOrder"),
                "OrdOrderServiceImpl.createOrder must have @Master alongside @Transactional");
        Path relay = findFile("scm-analytics/service/src/main/java/com/scmcloud/analytics/ingest/OutboxRelayJob.java");
        assertNotNull(relay, "OutboxRelayJob must exist");
        String relayContent = Files.readString(relay, StandardCharsets.UTF_8);
        assertTrue(relayContent.contains("@DistributedLockAnnotation") && relayContent.contains("outbox:relay"),
                "OutboxRelayJob.relay() must have active @DistributedLockAnnotation(key = \"'outbox:relay'\")");
        assertFalse(relayContent.contains("// @DistributedLock"),
                "OutboxRelayJob must not have commented-out DistributedLock — annotation must be active");
    }

    // ---- helpers ----

    private Path findOutboxSql() {
        String userDir = System.getProperty("user.dir");
        Path[] candidates = {
                Paths.get(userDir).resolve("scripts/db/microservices/041_outbox.sql"),
                Paths.get("scripts/db/microservices/041_outbox.sql").toAbsolutePath(),
                Paths.get(userDir).resolve("scm-analytics/service/scripts/db/microservices/041_outbox.sql"),
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        Path cur = Paths.get(userDir);
        for (int i = 0; i < 6 && cur != null; i++) {
            Path tryPath = cur.resolve("scripts/db/microservices/041_outbox.sql");
            if (Files.exists(tryPath)) return tryPath;
            cur = cur.getParent();
        }
        return null;
    }

    private Path findFile(String relative) {
        String userDir = System.getProperty("user.dir");
        Path[] candidates = {
                Paths.get(userDir).resolve(relative),
                Paths.get(relative).toAbsolutePath(),
        };
        for (Path p : candidates) {
            if (Files.exists(p)) return p;
        }
        Path cur = Paths.get(userDir);
        for (int i = 0; i < 6 && cur != null; i++) {
            Path tryPath = cur.resolve(relative);
            if (Files.exists(tryPath)) return tryPath;
            cur = cur.getParent();
        }
        return null;
    }
}
