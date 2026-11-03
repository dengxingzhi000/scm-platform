package com.scmcloud.gateway.util;

import lombok.Getter;
import lombok.NonNull;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

/**
 * Simple request decorator that keeps a copy of the request body for multiple reads.
 * 绠€鍗曠殑璇锋眰瑁呴グ鍣紝淇濈暀璇锋眰姝ｆ枃鐨勫壇鏈互渚涘娆¤鍙栵拷
 *
 * <p>Performance optimization: Uses read-only mode by default to avoid unnecessary defensive copies.
 * For scenarios requiring mutable buffers, use {@code readOnly = false}.
 */
@Getter
public class CachedBodyRequestDecorator extends ServerHttpRequestDecorator {
    private static final DataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

    private final byte[] cachedBody;
    private final boolean readOnly;

    /**
     * Creates a read-only cached body decorator (recommended for most use cases).
     */
    public CachedBodyRequestDecorator(ServerHttpRequest delegate, byte[] cachedBody) {
        this(delegate, cachedBody, true);
    }

    /**
     * Creates a cached body decorator with configurable read-only mode.
     *
     * @param delegate the original request
     * @param cachedBody the cached body bytes
     * @param readOnly if true, wraps original array directly (no copy); if false, creates defensive copies
     */
    public CachedBodyRequestDecorator(ServerHttpRequest delegate, byte[] cachedBody, boolean readOnly) {
        super(delegate);
        this.cachedBody = cachedBody != null ? cachedBody : new byte[0];
        this.readOnly = readOnly;
    }

    @Override
    @NonNull
    public Flux<DataBuffer> getBody() {
        return Flux.defer(() -> {
            if (readOnly) {
                // Read-only mode: wrap original array directly (no copy)
                // Note: Callers must not modify the buffer
                DataBuffer buffer = BUFFER_FACTORY.wrap(cachedBody);
                return Flux.just(buffer);
            } else {
                // Mutable mode: create defensive copy
                byte[] copy = new byte[cachedBody.length];
                System.arraycopy(cachedBody, 0, copy, 0, cachedBody.length);
                DataBuffer buffer = BUFFER_FACTORY.wrap(copy);
                return Flux.just(buffer);
            }
        });
    }
}