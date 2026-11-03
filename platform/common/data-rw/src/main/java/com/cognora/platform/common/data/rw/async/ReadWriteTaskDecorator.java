package com.cognora.platform.common.data.rw.async;

import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskDecorator;

/**
 * Propagates the read-write routing context from the submitting thread to the
 * worker thread.
 * <p>
 * Without it an async task starts in {@code AUTO} and, in addition, loses the
 * read-after-write marker — a write followed by an async read could observe a
 * stale replica. It is registered as the {@code TaskDecorator} bean, which
 * Spring Boot applies to its {@code applicationTaskExecutor}/default {@code @Async}
 * executor. Executors created by hand must wrap their tasks with
 * {@link ReadWriteRoutingContext#wrap(Runnable)} or install this decorator
 * explicitly.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        ReadWriteRoutingContext.Snapshot captured = ReadWriteRoutingContext.capture();
        return () -> {
            ReadWriteRoutingContext.Snapshot previous = ReadWriteRoutingContext.capture();
            try {
                ReadWriteRoutingContext.restore(captured);
                runnable.run();
            } catch (RuntimeException e) {
                log.debug("[RW-Async] Task failed with routing context {}: {}",
                        captured.type(), e.getMessage());
                throw e;
            } finally {
                ReadWriteRoutingContext.restore(previous);
            }
        };
    }
}
