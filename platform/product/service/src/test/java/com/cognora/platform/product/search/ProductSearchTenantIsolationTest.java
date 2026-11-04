package com.cognora.platform.product.search;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.web.domain.SecurityUser;
import com.cognora.platform.product.search.document.ProductDocument;
import com.cognora.platform.product.search.dto.ProductSearchRequest;
import com.cognora.platform.product.search.repository.ProductSearchRepository;
import com.cognora.platform.product.search.service.ProductSearchService;
import com.cognora.platform.product.support.ProductTenantResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * scm_product 是共享索引，所有查询都必须带当前租户。
 */
class ProductSearchTenantIsolationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private ProductSearchRepository repository;
    private ProductSearchService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProductSearchRepository.class);
        service = new ProductSearchService(repository, new ProductTenantResolver());
        when(repository.fullTextSearch(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        when(repository.findByTenantIdAndStatusOrderByTotalSalesDesc(anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        when(repository.findByTenantIdAndStatusOrderByPublishedAtDesc(anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        when(repository.findByTenantIdAndCategoryIdAndStatus(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        when(repository.findByTenantIdAndBrandIdAndStatus(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
        when(repository.advancedSearch(anyString(), any(), any(), any(), any(), anyString(), any(), any(Pageable.class)))
                .thenReturn(emptyPage());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    private static Page<ProductDocument> emptyPage() {
        return new PageImpl<>(List.of());
    }

    private void authenticate(UUID tokenTenant, UUID headerTenant) {
        SecurityUser user = SecurityUser.builder().userId(UUID.randomUUID()).username("tester")
                .tenantId(tokenTenant).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        if (headerTenant != null) {
            TenantContextHolder.setTenantId(headerTenant);
        }
    }

    @Test
    void keywordSearchIsScopedToTheAuthenticatedTenant() {
        authenticate(TENANT_A, TENANT_A);
        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("phone");

        service.search(request);

        verify(repository).fullTextSearch(eq("phone"), eq(TENANT_A.toString()), eq(1), any(Pageable.class));
    }

    @Test
    void advancedSearchIsScopedToTheAuthenticatedTenant() {
        authenticate(TENANT_A, TENANT_A);
        ProductSearchRequest request = new ProductSearchRequest();
        request.setBrandId("brand-1");

        service.search(request);

        verify(repository).advancedSearch(anyString(), any(), any(), any(), any(),
                eq(TENANT_A.toString()), eq(1), any(Pageable.class));
    }

    @Test
    void brandQueryIsScopedToTheAuthenticatedTenant() {
        authenticate(TENANT_A, TENANT_A);
        service.findByBrand("brand-1", 1, 20);

        verify(repository).findByTenantIdAndBrandIdAndStatus(
                eq(TENANT_A.toString()), eq("brand-1"), eq(1), any(Pageable.class));
    }

    @Test
    void categoryQueryIsScopedToTheAuthenticatedTenant() {
        authenticate(TENANT_A, TENANT_A);
        service.findByCategory("cat-1", 1, 20);

        verify(repository).findByTenantIdAndCategoryIdAndStatus(
                eq(TENANT_A.toString()), eq("cat-1"), eq(1), any(Pageable.class));
    }

    @Test
    void hotAndLatestListsAreScopedToTheAuthenticatedTenant() {
        authenticate(TENANT_A, TENANT_A);
        service.getHotProducts(1, 20);
        service.getLatestProducts(1, 20);

        verify(repository).findByTenantIdAndStatusOrderByTotalSalesDesc(
                eq(TENANT_A.toString()), eq(1), any(Pageable.class));
        verify(repository).findByTenantIdAndStatusOrderByPublishedAtDesc(
                eq(TENANT_A.toString()), eq(1), any(Pageable.class));
    }

    @Test
    void rejectsSearchWhenNoTenantContextExists() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();

        assertThrows(AccessDeniedException.class, () -> service.findByBrand("brand-1", 1, 20));
        verify(repository, never()).findByTenantIdAndBrandIdAndStatus(any(), any(), any(), any());
    }

    @Test
    void rejectsSearchWhenTokenTenantDiffersFromHeaderTenant() {
        authenticate(TENANT_A, TENANT_B);

        assertThrows(AccessDeniedException.class, () -> service.search(new ProductSearchRequest()));
        verify(repository, never()).fullTextSearch(any(), any(), any(), any());
    }
}
