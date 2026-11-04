package com.cognora.ecom.search.service;

import com.cognora.ecom.search.domain.ProductDocument;
import com.cognora.ecom.search.repository.ProductDocumentRepository;
import com.cognora.ecom.search.service.impl.ProductIndexServiceImpl;
import com.cognora.ecom.search.service.impl.ProductSearchServiceImpl;
import com.cognora.ecom.search.tenant.SearchTenantResolver;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductTenantIsolationTest {

    private static final UUID TENANT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID TENANT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    private final ProductDocumentRepository repository = Mockito.mock(ProductDocumentRepository.class);
    private final ElasticsearchOperations elasticsearchOperations = Mockito.mock(ElasticsearchOperations.class);
    private final SearchTenantResolver tenantResolver = new SearchTenantResolver();

    private ProductSearchServiceImpl searchService;
    private ProductIndexServiceImpl indexService;

    @BeforeEach
    void setUp() {
        searchService = new ProductSearchServiceImpl(repository, elasticsearchOperations,
                Mockito.mock(IProductIndexService.class), tenantResolver);
        indexService = new ProductIndexServiceImpl(repository, tenantResolver);
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    @Test
    void searchProductsQueriesOnlyTheCurrentTenant() {
        ProductDocument tenantADocument = document("A-1", 100L, TENANT_A.toString(), "active");
        stubSearchHits(tenantADocument);
        authenticate(TENANT_A);

        List<ProductDocument> results = searchService.searchProducts("phone", null, null, 0, 20);

        assertThat(results).hasSize(1);

        ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(ProductDocument.class));
        co.elastic.clients.elasticsearch._types.query_dsl.Query query =
                ((NativeQuery) queryCaptor.getValue()).getQuery();

        assertThat(query.isBool()).isTrue();
        assertThat(query.bool().filter())
                .anyMatch(filter -> filter.isTerm()
                        && "tenant_id".equals(filter.term().field())
                        && TENANT_A.toString().equals(filter.term().value().stringValue()));
    }

    @Test
    void similarProductsUsesTenantScopedRepository() {
        ProductDocument source = document("A-1", 100L, TENANT_A.toString(), "active");
        source.setCategoryId(10L);
        ProductDocument candidate = document("A-2", 101L, TENANT_A.toString(), "active");
        candidate.setCategoryId(10L);
        when(repository.findByTenantIdAndProductId(TENANT_A.toString(), 100L)).thenReturn(List.of(source));
        when(repository.findByTenantIdAndCategoryId(TENANT_A.toString(), 10L)).thenReturn(List.of(candidate));
        authenticate(TENANT_A);

        List<ProductDocument> results = searchService.getSimilarProducts(100L, 10);

        assertThat(results).extracting(ProductDocument::getTenantId)
                .containsOnly(TENANT_A.toString());
        verify(repository).findByTenantIdAndProductId(TENANT_A.toString(), 100L);
        verify(repository).findByTenantIdAndCategoryId(TENANT_A.toString(), 10L);
        verify(repository, never()).findByTenantIdAndProductId(TENANT_B.toString(), 100L);
    }

    @Test
    void personalizedResultsUseTenantScopedRepository() {
        ProductDocument tenantADocument = document("A-1", 100L, TENANT_A.toString(), "active");
        when(repository.findByTenantIdAndStatus(TENANT_A.toString(), "active"))
                .thenReturn(List.of(tenantADocument));
        authenticate(TENANT_A);

        List<ProductDocument> results = searchService.getPersonalizedResults("user-1", 20);

        assertThat(results).extracting(ProductDocument::getTenantId)
                .containsOnly(TENANT_A.toString());
        verify(repository).findByTenantIdAndStatus(TENANT_A.toString(), "active");
        verify(repository, never()).findByTenantIdAndStatus(TENANT_B.toString(), "active");
    }

    @Test
    void indexProductOverridesClientSuppliedTenant() {
        ProductDocument document = document("B-1", 200L, TENANT_B.toString(), "active");
        authenticate(TENANT_A);

        searchService.indexProduct(document);

        ArgumentCaptor<ProductDocument> captor = ArgumentCaptor.forClass(ProductDocument.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_A.toString());
    }

    @Test
    void bulkIndexStampsCurrentTenantOnEveryDocument() {
        ProductDocument first = document("B-1", 200L, TENANT_B.toString(), "active");
        ProductDocument second = document(null, 201L, null, "active");
        authenticate(TENANT_A);

        indexService.bulkIndex(List.of(first, second));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ProductDocument::getTenantId)
                .containsOnly(TENANT_A.toString());
    }

    @Test
    void deleteOnlyRemovesDocumentsOfTheCurrentTenant() {
        ProductDocument tenantADocument = document("A-1", 100L, TENANT_A.toString(), "active");
        when(repository.findByTenantIdAndProductId(TENANT_A.toString(), 100L))
                .thenReturn(List.of(tenantADocument));
        authenticate(TENANT_A);

        indexService.deleteProduct(TENANT_A.toString(), 100L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAll(captor.capture());
        assertThat(captor.getValue()).extracting(ProductDocument::getTenantId)
                .containsOnly(TENANT_A.toString());
        verify(repository, never()).findByTenantIdAndProductId(TENANT_B.toString(), 100L);
    }

    @Test
    void reindexStaysInsideTheCurrentTenant() {
        ProductDocument tenantADocument = document("A-1", 100L, TENANT_A.toString(), "active");
        when(repository.findByTenantId(TENANT_A.toString())).thenReturn(List.of(tenantADocument));

        int count = indexService.reindexAll(TENANT_A.toString());

        assertThat(count).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(ProductDocument::getTenantId)
                .containsOnly(TENANT_A.toString());
        verify(repository, never()).findByTenantId(TENANT_B.toString());
    }

    @Test
    void rejectsSearchWhenNoTenantContextExists() {
        assertThatThrownBy(() -> searchService.searchProducts("phone", null, null, 0, 20))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("required");
    }

    @Test
    void rejectsSearchWhenTokenTenantDiffersFromHeaderTenant() {
        authenticate(TENANT_A);
        TenantContextHolder.setTenantId(TENANT_B);

        assertThatThrownBy(() -> searchService.getPersonalizedResults("user-1", 20))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("does not match");
    }

    private void stubSearchHits(ProductDocument document) {
        SearchHit<ProductDocument> hit = Mockito.mock(SearchHit.class);
        when(hit.getContent()).thenReturn(document);
        SearchHits<ProductDocument> hits = Mockito.mock(SearchHits.class);
        when(hits.getSearchHits()).thenReturn(List.of(hit));
        when(elasticsearchOperations.search(any(Query.class), eq(ProductDocument.class))).thenReturn(hits);
    }

    private static ProductDocument document(String id, Long productId, String tenantId, String status) {
        ProductDocument document = new ProductDocument();
        document.setId(id);
        document.setProductId(productId);
        document.setTenantId(tenantId);
        document.setStatus(status);
        return document;
    }

    static void authenticate(UUID tenantId) {
        com.cognora.platform.common.web.domain.SecurityUser principal =
                com.cognora.platform.common.web.domain.SecurityUser.builder()
                        .userId(UUID.randomUUID())
                        .username("tester")
                        .tenantId(tenantId)
                        .roles(Set.of())
                        .permissions(Set.of())
                        .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
