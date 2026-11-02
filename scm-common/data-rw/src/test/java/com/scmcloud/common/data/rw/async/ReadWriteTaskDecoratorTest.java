package com.scmcloud.common.data.rw.async;

import com.scmcloud.common.data.rw.routing.ReadWriteRoutingContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("异步路由上下文传播测试")
class ReadWriteTaskDecoratorTest {

    private final ReadWriteTaskDecorator decorator = new ReadWriteTaskDecorator();

    @AfterEach
    void tearDown() {
        ReadWriteRoutingContext.clear();
    }

    @Test
    @DisplayName("decorate 应把主库/写标记传播到工作线程，并在执行后清理")
    void shouldPropagateMasterAndWriteMarker() throws Exception {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.MASTER);
        ReadWriteRoutingContext.markWrite();

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> inside = new AtomicReference<>();

            pool.submit(decorator.decorate(() -> inside.set(ReadWriteRoutingContext.current().name() + "|"
                    + (ReadWriteRoutingContext.getLastWriteTime() != null)))).get(5, TimeUnit.SECONDS);
            String after = pool.submit(() -> ReadWriteRoutingContext.current().name() + "|"
                    + (ReadWriteRoutingContext.getLastWriteTime() != null)).get(5, TimeUnit.SECONDS);

            assertEquals("MASTER|true", inside.get());
            assertEquals("AUTO|false", after, "任务结束后工作线程不能残留主库/写标记");
        } finally {
            pool.shutdownNow();
            ReadWriteRoutingContext.clear();
        }
    }

    @Test
    @DisplayName("decorate 应恢复工作线程原有的上下文，不污染复用线程")
    void shouldRestorePreviousWorkerContext() throws Exception {
        ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "slave1", true);
        Runnable decorated = decorator.decorate(() -> {
        });

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<String> workerSlave = new AtomicReference<>();
            pool.submit(() -> {
                // 模拟线程复用时工作线程已有的上下文
                ReadWriteRoutingContext.push(ReadWriteRoutingContext.RoutingType.SLAVE, "worker", true);
                decorated.run();
                workerSlave.set(ReadWriteRoutingContext.getSpecifiedSlave());
                ReadWriteRoutingContext.clear();
            }).get(5, TimeUnit.SECONDS);

            assertEquals("worker", workerSlave.get(), "装饰后的任务结束后必须还原工作线程原状态");
        } finally {
            pool.shutdownNow();
            ReadWriteRoutingContext.clear();
        }
    }
}
