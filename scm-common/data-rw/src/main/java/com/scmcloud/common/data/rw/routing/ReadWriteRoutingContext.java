package com.scmcloud.common.data.rw.routing;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Read-write routing context.
 * <p>
 * Uses ThreadLocal to store current thread routing info.
 * Supports nested calls (using stack structure).
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingContext {

    private static final ThreadLocal<Deque<RoutingType>> ROUTING_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private static final ThreadLocal<Instant> LAST_WRITE_TIME = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> FORCE_MASTER = ThreadLocal.withInitial(() -> false);

    private static final ThreadLocal<String> SPECIFIED_SLAVE = new ThreadLocal<>();

    public enum RoutingType {
        MASTER,
        SLAVE,
        AUTO
    }

    public static void push(RoutingType type) {
        ROUTING_STACK.get().push(type);
        log.trace("[RW-Routing] Push routing type: {}", type);
    }

    public static void pop() {
        Deque<RoutingType> stack = ROUTING_STACK.get();
        if (!stack.isEmpty()) {
            RoutingType popped = stack.pop();
            log.trace("[RW-Routing] Pop routing type: {}", popped);
        }
        if (stack.isEmpty()) {
            ROUTING_STACK.remove();
            SPECIFIED_SLAVE.remove();
        }
    }

    public static RoutingType current() {
        Deque<RoutingType> stack = ROUTING_STACK.get();
        return stack.isEmpty() ? RoutingType.AUTO : stack.peek();
    }

    public static void forceMaster() {
        FORCE_MASTER.set(true);
        log.trace("[RW-Routing] Force master enabled");
    }

    public static void clearForceMaster() {
        FORCE_MASTER.set(false);
        log.trace("[RW-Routing] Force master cleared");
    }

    public static boolean isForceMaster() {
        return Boolean.TRUE.equals(FORCE_MASTER.get());
    }

    public static void markWrite() {
        LAST_WRITE_TIME.set(Instant.now());
        log.trace("[RW-Routing] Write operation marked");
    }

    public static Instant getLastWriteTime() {
        return LAST_WRITE_TIME.get();
    }

    public static void specifySlave(String slaveName) {
        SPECIFIED_SLAVE.set(slaveName);
    }

    public static String getSpecifiedSlave() {
        return SPECIFIED_SLAVE.get();
    }

    public static void clear() {
        ROUTING_STACK.remove();
        LAST_WRITE_TIME.remove();
        FORCE_MASTER.remove();
        SPECIFIED_SLAVE.remove();
        log.trace("[RW-Routing] Context cleared");
    }

    /**
     * Check if should use master.
     *
     * @param readMasterAfterWriteMs time window for read-after-write consistency
     * @return true if should use master
     */
    public static boolean shouldUseMaster(long readMasterAfterWriteMs) {
        if (isForceMaster()) {
            return true;
        }

        if (current() == RoutingType.MASTER) {
            return true;
        }

        Instant lastWrite = getLastWriteTime();
        if (lastWrite != null) {
            long elapsed = Instant.now().toEpochMilli() - lastWrite.toEpochMilli();
            if (elapsed < readMasterAfterWriteMs) {
                log.debug("[RW-Routing] Using master due to recent write ({}ms ago)", elapsed);
                return true;
            }
        }

        return false;
    }
}
