package com.cognora.ecom.search.service.dubbo;

import com.cognora.ecom.search.api.dto.ProductSearchItem;
import com.cognora.ecom.search.domain.ProductDocument;
import com.cognora.ecom.search.service.IProductIndexService;
import com.cognora.ecom.search.service.IProductSearchService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchDubboServiceImplTenantTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");

    private final IProductSearchService searchService = Mockito.mock(IProductSearchService.class);
    private final IProductIndexService indexService = Mockito.mock(IProductIndexService.class);
    private SearchDubboServiceImpl dubboService;

    @BeforeEach
    void setUp() {
        dubboService = new SearchDubboServiceImpl(searchService, indexService);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();
    }

    @Test
    void dubboSearchRunsInsideTheProvidedTenantContext() {
        ProductDocument document = new ProductDocument();
        document.setId("A-1");
        document.setTenantId(TENANT_A.toString());
        when(searchService.searchProducts(eq("phone"), any(), any(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    assertThat(TenantContextHolder.getTenantId()).isEqualTo(TENANT_A);
                    return List.of(document);
                });

        List<ProductSearchItem> results = dubboService.searchProducts("phone", null, null, 0, 20,
                TENANT_A.toString());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_A.toString());
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void indexProductRequiresTenant() {
        ProductSearchItem item = new ProductSearchItem();
        item.setProductId(100L);

        assertThatThrownBy(() -> dubboService.indexProduct(item))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
        verify(searchService, Mockito.never()).indexProduct(any());
    }

    @Test
    void indexProductCarriesTheCallerTenant() {
        ProductSearchItem item = new ProductSearchItem();
        item.setId("A-1");
        item.setProductId(100L);
        item.setTenantId(TENANT_A.toString());

        dubboService.indexProduct(item);

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(searchService).indexProduct(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_A.toString());
    }

    @Test
    void removeProductRequiresTenant() {
        assertThatThrownBy(() -> dubboService.removeProduct(100L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
        verify(indexService, Mockito.never()).deleteProduct(any(), any());
    }

    @Test
    void rejectsMalformedTenant() {
        assertThatThrownBy(() -> dubboService.reindexAll("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a UUID");
    }
}
