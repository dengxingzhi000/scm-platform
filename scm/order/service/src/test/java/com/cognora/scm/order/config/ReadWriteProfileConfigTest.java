package com.cognora.scm.order.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 {@code application-readwrite.yml} 的结构与
 * {@code ReadWriteAutoConfiguration} 期望的配置形状一致。
 *
 * <p>这个 profile 默认不激活，CI 也不会启动 Spring 上下文，因此用一个
 * 纯解析测试兜住 YAML 语法和关键路径——配置写错时在测试阶段就失败，
 * 而不是等到第一次带 {@code --spring.profiles.active=readwrite} 启动才发现。</p>
 */
@DisplayName("读写分离 profile 配置测试")
class ReadWriteProfileConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadProfile() {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("application-readwrite.yml")) {
            assertNotNull(in, "application-readwrite.yml 必须在 classpath 上");
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new IllegalStateException("无法解析 application-readwrite.yml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> datasourceSection() {
        Map<String, Object> root = loadProfile();
        Map<String, Object> spring = (Map<String, Object>) root.get("spring");
        assertNotNull(spring, "缺少 spring 节点");
        Map<String, Object> datasource = (Map<String, Object>) spring.get("datasource");
        assertNotNull(datasource, "缺少 spring.datasource 节点");
        return datasource;
    }

    @Test
    @DisplayName("应启用读写分离并把路由 group 设为默认数据源")
    @SuppressWarnings("unchecked")
    void shouldEnableReadWriteSeparation() {
        Map<String, Object> datasource = datasourceSection();

        Map<String, Object> dynamic = (Map<String, Object>) datasource.get("dynamic");
        assertNotNull(dynamic, "缺少 spring.datasource.dynamic 节点");
        assertEquals("order", dynamic.get("primary"),
                "primary 必须指向路由 group，否则注解栈没有消费者");

        Map<String, Object> rw = (Map<String, Object>) datasource.get("rw");
        assertNotNull(rw, "缺少 spring.datasource.rw 节点");
        assertEquals(Boolean.TRUE, rw.get("enabled"));
    }

    @Test
    @DisplayName("order group 必须包含主库与至少一个从库")
    @SuppressWarnings("unchecked")
    void shouldDefineMasterAndSlaveGroup() {
        Map<String, Object> datasource = datasourceSection();
        Map<String, Object> rw = (Map<String, Object>) datasource.get("rw");
        Map<String, Object> groups = (Map<String, Object>) rw.get("groups");
        assertNotNull(groups, "缺少 groups 节点");

        Map<String, Object> order = (Map<String, Object>) groups.get("order");
        assertNotNull(order, "缺少 order group");

        Map<String, Object> master = (Map<String, Object>) order.get("master");
        assertNotNull(master, "order group 缺少 master");
        assertTrue(String.valueOf(master.get("url")).contains("db_order"));
        assertTrue(master.containsKey("username"));

        List<Map<String, Object>> slaves = (List<Map<String, Object>>) order.get("slaves");
        assertNotNull(slaves, "order group 缺少 slaves");
        assertEquals(1, slaves.size());
        assertEquals("slave1", slaves.get(0).get("name"));
        assertTrue(String.valueOf(slaves.get(0).get("url")).contains("db_order"));
    }

    @Test
    @DisplayName("写后读窗口与复制延迟阈值必须可解析为时长")
    @SuppressWarnings("unchecked")
    void shouldDeclareConsistencyWindow() {
        Map<String, Object> datasource = datasourceSection();
        Map<String, Object> rw = (Map<String, Object>) datasource.get("rw");

        assertEquals("2s", rw.get("read-master-after-write"));
        assertEquals("1s", rw.get("replication-lag-tolerance"));
    }
}
