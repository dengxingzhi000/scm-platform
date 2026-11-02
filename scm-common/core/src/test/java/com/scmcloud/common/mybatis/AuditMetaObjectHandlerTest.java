package com.scmcloud.common.mybatis;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.scmcloud.common.domain.TenantId;
import com.scmcloud.common.tenant.TenantContextHolder;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuditMetaObjectHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("019a0aee-0000-7000-8000-000000000002");
    private static final UUID USER_ID = UUID.fromString("019a0aee-0000-7000-8000-000000000003");

    private final AuditMetaObjectHandler handler = new AuditMetaObjectHandler();

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        SecurityContextHolder.clearContext();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(USER_ID.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void insertAppendOnlyEntitySkipsMissingOptionalFields() {
        AppendOnlyEntity entity = new AppendOnlyEntity();
        entity.id = "audit-entry";
        MetaObject metaObject = metaObject(entity);
        assertThat(metaObject.hasGetter("updateTime")).isFalse();
        assertThat(metaObject.hasGetter("deleted")).isFalse();
        assertThat(metaObject.hasGetter("createBy")).isFalse();
        assertThat(metaObject.hasGetter("updateBy")).isFalse();

        assertDoesNotThrow(() -> handler.insertFill(metaObject));

        assertThat(entity.tenantId).isEqualTo(TenantId.from(TENANT_ID));
        assertThat(entity.id).isEqualTo("audit-entry");
        assertThat(entity.createTime).isNull();
    }

    @Test
    void updateAppendOnlyEntitySkipsMissingUpdateTime() {
        AppendOnlyEntity entity = new AppendOnlyEntity();
        entity.createTime = LocalDateTime.of(2025, 1, 24, 8, 0);
        MetaObject metaObject = metaObject(entity);

        assertDoesNotThrow(() -> handler.updateFill(metaObject));

        assertThat(entity.createTime).isEqualTo(LocalDateTime.of(2025, 1, 24, 8, 0));
        assertThat(entity.tenantId).isNull();
    }

    @Test
    void insertEntityWithoutTenantIdDoesNotThrow() {
        NoTenantEntity entity = new NoTenantEntity();
        entity.id = "global-entry";
        MetaObject metaObject = metaObject(entity);
        assertThat(metaObject.hasGetter("tenantId")).isFalse();

        assertDoesNotThrow(() -> handler.insertFill(metaObject));

        assertThat(entity.id).isEqualTo("global-entry");
        assertThat(TenantContextHolder.getRequiredTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void insertUuidTenantWithoutTimestampsFillsUuid() {
        UuidTenantEntity entity = new UuidTenantEntity();
        MetaObject metaObject = metaObject(entity);
        assertThat(metaObject.hasGetter("createTime")).isFalse();
        assertThat(metaObject.hasGetter("updateTime")).isFalse();

        assertDoesNotThrow(() -> handler.insertFill(metaObject));

        assertThat(entity.tenantId).isEqualTo(TENANT_ID);
    }

    @Test
    void insertFillsLocalTimestampsAndStringTenant() {
        LocalTimeEntity entity = new LocalTimeEntity();
        MetaObject metaObject = metaObject(entity);
        LocalDateTime before = LocalDateTime.now();

        handler.insertFill(metaObject);

        LocalDateTime after = LocalDateTime.now();
        assertThat(entity.createTime).isBetween(before, after);
        assertThat(entity.updateTime).isBetween(before, after);
        assertThat(entity.tenantId).isEqualTo(TENANT_ID.toString());
    }

    @Test
    void updateFillsLocalTimestampOnly() {
        LocalTimeEntity entity = new LocalTimeEntity();
        MetaObject metaObject = metaObject(entity);
        LocalDateTime before = LocalDateTime.now();

        handler.updateFill(metaObject);

        assertThat(entity.updateTime).isBetween(before, LocalDateTime.now());
        assertThat(entity.createTime).isNull();
        assertThat(entity.tenantId).isNull();
    }

    @Test
    void insertFillsOffsetTimestampsWithoutTenantField() {
        OffsetTimeEntity entity = new OffsetTimeEntity();
        MetaObject metaObject = metaObject(entity);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        handler.insertFill(metaObject);

        assertThat(entity.createTime).isBetween(before, OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(entity.updateTime).isEqualTo(entity.createTime);
        assertThat(entity.createTime.getOffset()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    void updateFillsOffsetTimestampOnly() {
        OffsetTimeEntity entity = new OffsetTimeEntity();
        MetaObject metaObject = metaObject(entity);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);

        handler.updateFill(metaObject);

        assertThat(entity.updateTime).isBetween(before, OffsetDateTime.now(ZoneOffset.UTC));
        assertThat(entity.updateTime.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(entity.createTime).isNull();
    }

    @Test
    void strictFillPreservesExistingTenantAndTimestamps() {
        LocalTimeEntity entity = new LocalTimeEntity();
        String existingTenant = USER_ID.toString();
        LocalDateTime existingTime = LocalDateTime.of(2025, 1, 24, 8, 0);
        entity.tenantId = existingTenant;
        entity.createTime = existingTime;
        entity.updateTime = existingTime;
        MetaObject metaObject = metaObject(entity);

        handler.insertFill(metaObject);
        handler.updateFill(metaObject);

        assertThat(entity.tenantId).isEqualTo(existingTenant);
        assertThat(entity.createTime).isEqualTo(existingTime);
        assertThat(entity.updateTime).isEqualTo(existingTime);
    }

    @Test
    void missingTenantContextDoesNotFabricateTenant() {
        TenantContextHolder.clear();
        AppendOnlyEntity entity = new AppendOnlyEntity();

        handler.insertFill(metaObject(entity));

        assertThat(entity.tenantId).isNull();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    private MetaObject metaObject(Object entity) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        assistant.setCurrentNamespace(entity.getClass().getName());
        TableInfoHelper.initTableInfo(assistant, entity.getClass());
        return configuration.newMetaObject(entity);
    }

    @Getter
    @Setter
    static class AppendOnlyEntity {
        private String id;

        @TableField(fill = FieldFill.INSERT)
        private TenantId tenantId;

        @TableField("create_time")
        private LocalDateTime createTime;
    }

    @Getter
    @Setter
    static class NoTenantEntity {
        private String id;
    }

    @Getter
    @Setter
    static class UuidTenantEntity {
        private String id;

        @TableField(fill = FieldFill.INSERT)
        private UUID tenantId;
    }

    @Getter
    @Setter
    static class LocalTimeEntity {
        private String id;

        @TableField(fill = FieldFill.INSERT)
        private String tenantId;

        @TableField(fill = FieldFill.INSERT)
        private LocalDateTime createTime;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;
    }

    @Getter
    @Setter
    static class OffsetTimeEntity {
        private String id;

        @TableField(fill = FieldFill.INSERT)
        private OffsetDateTime createTime;

        @TableField(fill = FieldFill.INSERT_UPDATE)
        private OffsetDateTime updateTime;
    }
}
