package com.scmcloud.common.data.rw.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("读写路由上下文测试")
class ReadWriteRoutingContextTest {

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    @Test
    @DisplayName("默认路由类型应该是 AUTO")
    void defaultRoutingTypeShouldBeAuto() {
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("应该支持 push 和 pop 操作")
    void shouldSupportPushAndPop() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER, ReadWriteRoutingContext.current());

        ReadWriteRoutingContext.pop();
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("应该支持嵌套路由")
    void shouldSupportNestedRouting() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER, ReadWriteRoutingContext.current());

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        assertEquals(ReadWriteRoutingContext.RoutingType.SLAVE, ReadWriteRoutingContext.current());

        ReadWriteRoutingContext.pop();
        assertEquals(ReadWriteRoutingContext.RoutingType.MASTER, ReadWriteRoutingContext.current());

        ReadWriteRoutingContext.pop();
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
    }

    @Test
    @DisplayName("应该支持强制主库")
    void shouldSupportForceMaster() {
        assertFalse(ReadWriteRoutingContext.isForceMaster());

        ReadWriteRoutingContext.forceMaster();
        assertTrue(ReadWriteRoutingContext.isForceMaster());

        ReadWriteRoutingContext.clearForceMaster();
        assertFalse(ReadWriteRoutingContext.isForceMaster());
    }

    @Test
    @DisplayName("应该支持指定从库")
    void shouldSupportSpecifiedSlave() {
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());

        ReadWriteRoutingContext.specifySlave("slave1");
        assertEquals("slave1", ReadWriteRoutingContext.getSpecifiedSlave());
    }

    @Test
    @DisplayName("应该支持标记写操作")
    void shouldSupportMarkWrite() {
        assertNull(ReadWriteRoutingContext.getLastWriteTime());

        ReadWriteRoutingContext.markWrite();
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime());
    }

    @Test
    @DisplayName("强制主库时 shouldUseMaster 应该返回 true")
    void shouldUseMasterWhenForced() {
        ReadWriteRoutingContext.forceMaster();
        assertTrue(ReadWriteRoutingContext.shouldUseMaster(0));
    }

    @Test
    @DisplayName("显式指定主库时 shouldUseMaster 应该返回 true")
    void shouldUseMasterWhenExplicit() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        assertTrue(ReadWriteRoutingContext.shouldUseMaster(0));
    }

    @Test
    @DisplayName("写后读应该返回主库")
    void shouldUseMasterAfterWrite() {
        ReadWriteRoutingContext.markWrite();
        assertTrue(ReadWriteRoutingContext.shouldUseMaster(5000)); // 5 秒内
    }

    @Test
    @DisplayName("超过时间窗口后应该返回从库")
    void shouldNotUseMasterAfterTimeout() {
        // 不标记写操作，应该返回 false
        assertFalse(ReadWriteRoutingContext.shouldUseMaster(0));
    }

    @Test
    @DisplayName("clear 应该清理所有上下文")
    void clearShouldCleanAll() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        ReadWriteRoutingContext.forceMaster();
        ReadWriteRoutingContext.specifySlave("slave1");
        ReadWriteRoutingContext.markWrite();

        ReadWriteRoutingContext.clear();

        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
        assertFalse(ReadWriteRoutingContext.isForceMaster());
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());
    }
}
