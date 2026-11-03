package com.cognora.platform.common.data.rw.tracing;

import com.cognora.platform.common.data.rw.routing.ReadWriteRoutingContext;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Read-write separation tracing interceptor.
 * <p>
 * Wraps the annotated service method in an OpenTelemetry span and records the
 * <b>actual</b> target chosen by the routing datasource:
 * <ul>
 *   <li>{@code db.rw.routing_type} — MASTER / SLAVE / AUTO at invocation start;</li>
 *   <li>{@code db.rw.target} — master or the concrete slave name;</li>
 *   <li>{@code db.rw.fallback} — true when a slave was requested but the request
 *       ended on master (unavailable, circuit open, replication lag);</li>
 *   <li>{@code db.rw.specified_slave} — slave name from {@code @Slave(value)}.</li>
 * </ul>
 * The same information is logged at DEBUG, so routing can be explained even when
 * no OpenTelemetry SDK/exporter is configured (the global tracer is a no-op then).
 *
 * @author Deng
 * @since 2025-12-16
 */
@Slf4j
public class ReadWriteTracingInterceptor implements MethodInterceptor {
    private static final String INSTRUMENTATION_NAME = "com.cognora.platform.common.data.rw";

    private final Tracer tracer;

    public ReadWriteTracingInterceptor() {
        this(GlobalOpenTelemetry.getTracer(INSTRUMENTATION_NAME));
    }

    public ReadWriteTracingInterceptor(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        String methodName = invocation.getMethod().getDeclaringClass().getSimpleName() +
                "." + invocation.getMethod().getName();

        ReadWriteRoutingContext.RoutingType routingType = ReadWriteRoutingContext.current();

        Span span = tracer.spanBuilder("db.rw." + methodName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.rw.routing_type", routingType.name())
                .setAttribute("db.rw.force_master", ReadWriteRoutingContext.isForceMaster())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            String specifiedSlave = ReadWriteRoutingContext.getSpecifiedSlave();
            if (specifiedSlave != null) {
                span.setAttribute("db.rw.specified_slave", specifiedSlave);
            }

            try {
                Object result = invocation.proceed();
                span.setStatus(StatusCode.OK);
                return result;
            } catch (Throwable e) {
                span.setStatus(StatusCode.ERROR, e.getMessage());
                span.recordException(e);
                throw e;
            } finally {
                recordRoutingOutcome(span, methodName, routingType);
            }
        } finally {
            span.end();
        }
    }

    private void recordRoutingOutcome(Span span, String methodName,
                                     ReadWriteRoutingContext.RoutingType routingType) {
        ReadWriteRoutingContext.RouteState route = ReadWriteRoutingContext.currentRoute();
        if (route == null || !route.recorded()) {
            return;
        }
        span.setAttribute("db.rw.target", route.target());
        span.setAttribute("db.rw.fallback", route.fallback());
        if (route.slave() != null) {
            span.setAttribute("db.rw.slave", route.slave());
        }
        log.debug("[RW-Trace] {} routing={} target={} slave={} fallback={}",
                methodName, routingType, route.target(), route.slave(), route.fallback());
    }
}
