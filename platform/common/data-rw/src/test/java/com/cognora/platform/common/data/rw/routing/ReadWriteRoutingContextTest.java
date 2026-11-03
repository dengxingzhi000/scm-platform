package com.cognora.platform.common.data.rw.routing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
    @DisplayName("嵌套 @Slave 结束后必须恢复外层指定的从库，不能把内层选择泄漏出去")
    void nestedSlaveSelectionShouldBeRestored() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);
        assertEquals("slave1", ReadWriteRoutingContext.getSpecifiedSlave());

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave2", true);
        assertEquals("slave2", ReadWriteRoutingContext.getSpecifiedSlave());

        ReadWriteRoutingContext.pop();
        assertEquals("slave1", ReadWriteRoutingContext.getSpecifiedSlave(),
                "内层 @Slave(value=slave2) 返回后，外层应重新使用 slave1");

        ReadWriteRoutingContext.pop();
        assertEquals("AUTO", ReadWriteRoutingContext.current().name());
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave(), "最外层结束后不能残留从库选择");
    }

    @Test
    @DisplayName("嵌套弹出不应清掉外层的写后读标记")
    void nestedPopShouldKeepParentWriteMarker() {
        ReadWriteRoutingContext.markWrite();
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, null, true);
        ReadWriteRoutingContext.pop();

        assertNotNull(ReadWriteRoutingContext.getLastWriteTime(),
                "内层作用域结束时不能清掉父作用域的写标记");
    }

    @Test
    @DisplayName("fallbackToMaster 默认 true，显式 false 时如实返回")
    void fallbackFlagShouldFollowScope() {
        assertTrue(ReadWriteRoutingContext.isFallbackToMaster());

        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", false);
        assertFalse(ReadWriteRoutingContext.isFallbackToMaster());
        assertEquals(false, ReadWriteRoutingContext.describe().get("fallbackToMaster"));

        ReadWriteRoutingContext.pop();
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
    @DisplayName("指定从库只作用于当前作用域")
    void shouldSupportSpecifiedSlave() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE);
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());

        ReadWriteRoutingContext.specifySlave("slave1");
        assertEquals("slave1", ReadWriteRoutingContext.getSpecifiedSlave());

        ReadWriteRoutingContext.pop();
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());
    }

    @Test
    @DisplayName("没有作用域时 specifySlave 不应污染线程")
    void specifySlaveWithoutScopeShouldBeIgnored() {
        ReadWriteRoutingContext.specifySlave("leaked");
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());
        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
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
    @DisplayName("异步线程默认没有上下文，落到 AUTO（数据源层再取 default-target）")
    void asyncThreadShouldStartWithoutContext() throws Exception {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        ReadWriteRoutingContext.markWrite();
        ReadWriteRoutingContext.beginRequestScope();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> routingType = new AtomicReference<>();
            pool.submit(() -> routingType.set(ReadWriteRoutingContext.current().name())).get(5, TimeUnit.SECONDS);

            assertEquals("AUTO", routingType.get(), "未传播上下文的异步线程不应继承主库/写标记");
        } finally {
            pool.shutdownNow();
            ReadWriteRoutingContext.clear();
        }
    }

    @Test
    @DisplayName("wrap 应把当前上下文传播到工作线程并在执行后清理")
    void wrapShouldPropagateContextAndCleanUp() throws Exception {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);
        ReadWriteRoutingContext.markWrite();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> inside = new AtomicReference<>();
            pool.submit(ReadWriteRoutingContext.wrap(() ->
                    inside.set(ReadWriteRoutingContext.getSpecifiedSlave() + "|"
                            + ReadWriteRoutingContext.current().name() + "|"
                            + (ReadWriteRoutingContext.getLastWriteTime() != null)))).get(5, TimeUnit.SECONDS);
            String after = pool.submit(() -> ReadWriteRoutingContext.current().name())
                    .get(5, TimeUnit.SECONDS);

            assertEquals("slave1|SLAVE|true", inside.get());
            assertEquals("AUTO", after, "任务结束后工作线程必须恢复到进入前的空上下文");
        } finally {
            pool.shutdownNow();
            ReadWriteRoutingContext.clear();
        }
    }

    @Test
    @DisplayName("capture/restore 应完整往返")
    void captureRestoreShouldRoundTrip() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave2", false);
        ReadWriteRoutingContext.markWrite();
        ReadWriteRoutingContext.Snapshot snapshot = ReadWriteRoutingContext.capture();

        ReadWriteRoutingContext.clear();
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        assertTrue(ReadWriteRoutingContext.isFallbackToMaster());

        ReadWriteRoutingContext.restore(snapshot);

        assertEquals(ReadWriteRoutingContext.RoutingType.SLAVE, ReadWriteRoutingContext.current());
        assertEquals("slave2", ReadWriteRoutingContext.getSpecifiedSlave());
        assertFalse(ReadWriteRoutingContext.isFallbackToMaster());
        assertNotNull(ReadWriteRoutingContext.getLastWriteTime());
    }

    @Test
    @DisplayName("clear 应该清理所有上下文")
    void clearShouldCleanAll() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        ReadWriteRoutingContext.forceMaster();
        ReadWriteRoutingContext.specifySlave("slave1");
        ReadWriteRoutingContext.markWrite();
        ReadWriteRoutingContext.beginRequestScope();

        ReadWriteRoutingContext.clear();

        assertEquals(ReadWriteRoutingContext.RoutingType.AUTO, ReadWriteRoutingContext.current());
        assertFalse(ReadWriteRoutingContext.isForceMaster());
        assertNull(ReadWriteRoutingContext.getSpecifiedSlave());
        assertFalse(ReadWriteRoutingContext.isRequestScope());
    }

    @Test
    @DisplayName("describe 应输出当前线程的路由状态")
    void describeShouldExposeState() {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave3", true);
        ReadWriteRoutingContext.beginRequestScope();

        assertEquals("SLAVE", ReadWriteRoutingContext.describe().get("routingType"));
        assertEquals("slave3", ReadWriteRoutingContext.describe().get("specifiedSlave"));
        assertEquals(true, ReadWriteRoutingContext.describe().get("requestScope"));
        assertEquals(1, ReadWriteRoutingContext.describe().get("scopeDepth"));
    }
}