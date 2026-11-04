package com.cognora.platform.product.sync;

import com.cognora.platform.product.search.document.ProductDocument;
import com.cognora.platform.product.search.repository.ProductSearchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 同步消费者写入共享索引时必须带 tenant_id，缺失则拒绝写入 / 删除。
 */
class ProductSyncConsumerTenantTest {

    private static final String TENANT_A = "00000000-0000-0000-0000-00000000000a";

    private ProductSearchRepository repository;
    private ProductSyncConsumer consumer;

    @BeforeEach
    void setUp() {
        repository = mock(ProductSearchRepository.class);
        consumer = new ProductSyncConsumer(repository, new ObjectMapper());
    }

    @Test
    void createStampsTenantFromTheChangeEvent() {
        consumer.consumeProductChange("""
                {"op":"c","after":{"id":"spu-1","tenant_id":"%s","spu_name":"Phone","status":1}}
                """.formatted(TENANT_A));

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(TENANT_A, captor.getValue().getTenantId());
    }

    @Test
    void updateKeepsTenantFromTheChangeEvent() {
        consumer.consumeProductChange("""
                {"op":"u","after":{"id":"spu-2","tenant_id":"%s","spu_name":"Phone","status":1}}
                """.formatted(TENANT_A));

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(repository).save(captor.capture());
        assertEquals(TENANT_A, captor.getValue().getTenantId());
    }

    @Test
    void createWithoutTenantIsRejected() {
        consumer.consumeProductChange("""
                {"op":"c","after":{"id":"spu-3","spu_name":"Phone","status":1}}
                """);

        verify(repository, never()).save(any(ProductDocument.class));
    }

    @Test
    void deleteIsScopedToTheEventTenant() {
        consumer.consumeProductChange("""
                {"op":"d","before":{"id":"spu-4","tenant_id":"%s"}}
                """.formatted(TENANT_A));

        verify(repository).deleteByTenantIdAndId(TENANT_A, "spu-4");
        verify(repository, never()).deleteById(anyString());
    }

    @Test
    void deleteWithoutTenantIsRejected() {
        consumer.consumeProductChange("""
                {"op":"d","before":{"id":"spu-5"}}
                """);

        verify(repository, never()).deleteByTenantIdAndId(anyString(), anyString());
        verify(repository, never()).deleteById(anyString());
    }
}
