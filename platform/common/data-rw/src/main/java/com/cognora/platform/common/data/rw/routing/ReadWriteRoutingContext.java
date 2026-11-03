package com.cognora.platform.common.data.rw.routing;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Read-write routing context.
 * <p>
 * Uses ThreadLocal to store current thread routing info.
 * Supports nested calls (using stack structure). Every stack frame carries the
 * slave name and the fallback flag of that scope, so a nested scope can no
 * longer leak its slave selection into the outer scope.
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteRoutingContext {

    private static final ThreadLocal<Deque<Frame>> ROUTING_STACK = new ThreadLocal<>();

    private static final ThreadLocal<Instant> LAST_WRITE_TIME = new ThreadLocal<>();

    private static final ThreadLocal<Boolean> FORCE_MASTER = ThreadLocal.withInitial(() -> false);

    /**
     * Marks a servlet request thread. Only request threads allow the SQL layer to
     * send unannotated SELECTs to a slave; async / scheduled / startup threads
     * keep the safe master default.
     */
    private static final ThreadLocal<Boolean> REQUEST_SCOPE = new ThreadLocal<>();

    /**
     * Last routing outcome of a thread that has no active scope (AUTO / SQL layer),
     * kept only for diagnostics such as {@code /actuator/readwrite}.
     */
    private static final ThreadLocal<RouteState> DETACHED_ROUTE = new ThreadLocal<>();

    public enum RoutingType {
        MASTER,
        SLAVE,
        AUTO
    }

    /**
     * Mutable routing outcome of one scope. The datasource records the actual target
     * (which may be a fallback to master) so tracing and diagnostics can explain why
     * a read did not hit a replica.
     */
    public static final class RouteState {
        private volatile String target;
        private volatile String slave;
        private volatile boolean fallback;

        public void update(String target, String slave, boolean fallback) {
            this.target = target;
            this.slave = slave;
            this.fallback = fallback;
        }

        public String target() {
            return target;
        }

        public String slave() {
            return slave;
        }

        public boolean fallback() {
            return fallback;
        }

        public boolean recorded() {
            return target != null;
        }
    }

    /**
     * One routing scope.
     *
     * @param type             routing decision of the scope
     * @param specifiedSlave   slave requested by the annotation / SQL hint, null for "any"
     * @param fallbackToMaster whether an unavailable slave may fall back to master
     * @param route            actual target recorded by the datasource
     */
    public record Frame(RoutingType type, String specifiedSlave, boolean fallbackToMaster,
                        RouteState route) {

        public Frame(RoutingType type, String specifiedSlave, boolean fallbackToMaster) {
            this(type, specifiedSlave, fallbackToMaster, new RouteState());
        }
    }

    /**
     * Point-in-time snapshot of the context, used to propagate the routing
     * decision to another thread.
     */
    public record Snapshot(RoutingType type, String specifiedSlave, boolean fallbackToMaster,
                           boolean forceMaster, Instant lastWriteTime) {

        public static Snapshot empty() {
            return new Snapshot(RoutingType.AUTO, null, true, false, null);
        }
    }

    public static void push(RoutingType type) {
        push(type, null, true);
    }

    public static void push(RoutingType type, String specifiedSlave, boolean fallbackToMaster) {
        Deque<Frame> stack = stack(true);
        stack.push(new Frame(type, specifiedSlave, fallbackToMaster));
        log.trace("[RW-Routing] Push routing type: {}, slave: {}, fallback: {}",
                type, specifiedSlave, fallbackToMaster);
    }

    public static void pop() {
        Deque<Frame> stack = stack(false);
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Frame popped = stack.pop();
        if (popped.route() != null && popped.route().recorded()) {
            DETACHED_ROUTE.set(popped.route());
        }
        if (stack.isEmpty()) {
            // Drop the stack only. The read-after-write marker must survive the
            // pop of the command method that produced it, otherwise the
            // read-master-after-write window would never take effect.
            ROUTING_STACK.remove();
        }
        log.trace("[RW-Routing] Pop routing type: {}", popped.type());
    }

    public static RoutingType current() {
        Frame frame = currentFrame();
        return frame == null ? RoutingType.AUTO : frame.type();
    }

    public static String getSpecifiedSlave() {
        Frame frame = currentFrame();
        return frame == null ? null : frame.specifiedSlave();
    }

    /**
     * Whether the current scope allows falling back to master when the slave is unavailable.
     */
    public static boolean isFallbackToMaster() {
        Frame frame = currentFrame();
        return frame == null || frame.fallbackToMaster();
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

    /**
     * Select a concrete slave for the current scope. It only affects the top frame —
     * nested scopes and the parent scope keep their own selection.
     */
    public static void specifySlave(String slaveName) {
        Deque<Frame> stack = stack(false);
        if (stack == null || stack.isEmpty()) {
            log.debug("[RW-Routing] Ignoring slave selection [{}]: no active routing scope", slaveName);
            return;
        }
        Frame top = stack.pop();
        stack.push(new Frame(top.type(), slaveName, top.fallbackToMaster(), top.route()));
    }

    /**
     * Record the actual routing target chosen by the datasource. A fallback to master
     * is recorded with {@code fallback=true} so tracing can explain the decision.
     */
    public static void recordRoute(String target, String slaveName, boolean fallback) {
        Frame frame = currentFrame();
        if (frame != null) {
            frame.route().update(target, slaveName, fallback);
        } else {
            RouteState state = new RouteState();
            state.update(target, slaveName, fallback);
            DETACHED_ROUTE.set(state);
        }
    }

    /**
     * Routing outcome of the current scope, or of the last completed scope when the
     * caller runs outside one.
     */
    public static RouteState currentRoute() {
        Frame frame = currentFrame();
        return frame != null ? frame.route() : DETACHED_ROUTE.get();
    }

    public static void clear() {
        ROUTING_STACK.remove();
        LAST_WRITE_TIME.remove();
        FORCE_MASTER.remove();
        REQUEST_SCOPE.remove();
        DETACHED_ROUTE.remove();
        log.trace("[RW-Routing] Context cleared");
    }

    /**
     * Marks the current thread as serving a request. Called by the servlet filter so
     * that unannotated SELECTs may use a replica during a request only.
     */
    public static void beginRequestScope() {
        REQUEST_SCOPE.set(true);
    }

    /**
     * Clears both the request marker and all routing state, so a pooled request
     * thread never leaks a slave selection or a write marker into the next request.
     */
    public static void endRequestScope() {
        clear();
    }

    public static boolean isRequestScope() {
        return Boolean.TRUE.equals(REQUEST_SCOPE.get());
    }

    /**
     * Capture the effective routing state of the current thread.
     */
    public static Snapshot capture() {
        Frame frame = currentFrame();
        return new Snapshot(
                current(),
                frame == null ? null : frame.specifiedSlave(),
                frame == null || frame.fallbackToMaster(),
                isForceMaster(),
                getLastWriteTime());
    }

    /**
     * Replace the routing state of the current thread with the given snapshot.
     */
    public static void restore(Snapshot snapshot) {
        clear();
        if (snapshot == null) {
            return;
        }
        if (snapshot.type() != null && snapshot.type() != RoutingType.AUTO) {
            push(snapshot.type(), snapshot.specifiedSlave(), snapshot.fallbackToMaster());
        }
        if (snapshot.forceMaster()) {
            forceMaster();
        }
        if (snapshot.lastWriteTime() != null) {
            LAST_WRITE_TIME.set(snapshot.lastWriteTime());
        }
    }

    /**
     * Wrap a task so it runs with (and cleans up) the routing context of the
     * submitting thread. Use this for executors that are not covered by the
     * auto-configured {@code TaskDecorator}.
     */
    public static Runnable wrap(Runnable task) {
        Snapshot captured = capture();
        return () -> {
            Snapshot previous = capture();
            try {
                restore(captured);
                task.run();
            } finally {
                restore(previous);
            }
        };
    }

    /**
     * Callable variant of {@link #wrap(Runnable)}.
     */
    public static <T> Callable<T> wrap(Callable<T> task) {
        Snapshot captured = capture();
        return () -> {
            Snapshot previous = capture();
            try {
                restore(captured);
                return task.call();
            } finally {
                restore(previous);
            }
        };
    }

    /**
     * Human-readable state of the calling thread, exposed by the actuator endpoint
     * and used in diagnostic logs.
     */
    public static Map<String, Object> describe() {
        Map<String, Object> state = new LinkedHashMap<>();
        Frame frame = currentFrame();
        state.put("routingType", current().name());
        state.put("specifiedSlave", frame == null ? null : frame.specifiedSlave());
        state.put("fallbackToMaster", frame == null || frame.fallbackToMaster());
        state.put("forceMaster", isForceMaster());
        Instant lastWrite = getLastWriteTime();
        state.put("lastWriteTime", lastWrite == null ? null : lastWrite.toString());
        state.put("millisSinceLastWrite",
                lastWrite == null ? null : Duration.between(lastWrite, Instant.now()).toMillis());
        Deque<Frame> stack = stack(false);
        state.put("scopeDepth", stack == null ? 0 : stack.size());
        state.put("requestScope", isRequestScope());
        RouteState route = currentRoute();
        state.put("lastTarget", route == null ? null : route.target());
        state.put("lastSlave", route == null ? null : route.slave());
        state.put("lastFallback", route != null && route.fallback());
        return state;
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

    private static Frame currentFrame() {
        Deque<Frame> stack = stack(false);
        return stack == null || stack.isEmpty() ? null : stack.peek();
    }

    private static Deque<Frame> stack(boolean create) {
        Deque<Frame> stack = ROUTING_STACK.get();
        if (stack == null && create) {
            stack = new ArrayDeque<>();
            ROUTING_STACK.set(stack);
        }
        return stack;
    }
}