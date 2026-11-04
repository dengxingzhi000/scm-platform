package com.cognora.ecom.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.cognora.ecom.search.domain.ProductDocument;
import com.cognora.ecom.search.query.SearchQueryFactory;
import com.cognora.ecom.search.repository.ProductDocumentRepository;
import com.cognora.ecom.search.service.IProductIndexService;
import com.cognora.ecom.search.service.IProductSearchService;
import com.cognora.ecom.search.tenant.SearchTenantResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductSearchServiceImpl implements IProductSearchService {
    private final ProductDocumentRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final IProductIndexService productIndexService;
    private final SearchTenantResolver tenantResolver;

    @Override
    public List<ProductDocument> searchProducts(String keyword, Long categoryId, Long brandId, int page, int size) {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Searching products: tenantId={}, keyword={}, categoryId={}, brandId={}",
                tenantId, sanitize(keyword), sanitize(String.valueOf(categoryId)), sanitize(String.valueOf(brandId)));

        List<Query> filters = new ArrayList<>();
        filters.add(SearchQueryFactory.tenantTerm(tenantId));
        filters.add(Query.of(q -> q.term(t -> t.field("status").value("active"))));
        if (categoryId != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("category_id").value(String.valueOf(categoryId)))));
        }
        if (brandId != null) {
            filters.add(Query.of(q -> q.term(t -> t.field("brand_id").value(String.valueOf(brandId)))));
        }

        var bool = new co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery.Builder();
        filters.forEach(bool::filter);
        if (keyword != null && !keyword.isEmpty()) {
            bool.must(Query.of(q -> q.multiMatch(m -> m.query(keyword).fields("title", "description"))));
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .withPageable(PageRequest.of(page, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDocument> getSimilarProducts(Long productId, int limit) {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Getting similar products: tenantId={}, productId={}, limit={}", tenantId, productId, limit);
        List<ProductDocument> source = productRepository.findByTenantIdAndProductId(tenantId, productId);
        if (!source.isEmpty() && source.get(0).getCategoryId() != null) {
            Long categoryId = source.get(0).getCategoryId();
            return productRepository.findByTenantIdAndCategoryId(tenantId, categoryId).stream()
                    .filter(product -> !productId.equals(product.getProductId()))
                    .filter(product -> "active".equals(product.getStatus()))
                    .limit(limit)
                    .collect(Collectors.toList());
        }
        return productRepository.findByTenantIdAndStatus(tenantId, "active")
                .stream()
                .filter(product -> !productId.equals(product.getProductId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDocument> getPersonalizedResults(String userId, int limit) {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Getting personalized results: tenantId={}, userId={}, limit={}",
                tenantId, sanitize(userId), limit);
        return productRepository.findByTenantIdAndStatus(tenantId, "active")
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void indexProduct(ProductDocument product) {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Indexing product: tenantId={}, productId={}", tenantId, product.getProductId());
        applyTenant(product, tenantId);
        productRepository.save(product);
    }

    @Override
    public void removeProduct(Long productId) {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Removing product from index: tenantId={}, productId={}", tenantId, productId);
        productIndexService.deleteProduct(tenantId, productId);
    }

    @Override
    public void reindexAll() {
        String tenantId = tenantResolver.resolveTenantIdString();
        log.info("Reindexing all products: tenantId={}", tenantId);
        productIndexService.reindexAll(tenantId);
    }

    static void applyTenant(ProductDocument product, String tenantId) {
        if (product.getTenantId() != null && !tenantId.equals(product.getTenantId())) {
            log.warn("Ignoring client-supplied tenantId on product document: tenantId={}, supplied={}",
                    tenantId, sanitize(product.getTenantId()));
        }
        product.setTenantId(tenantId);
    }

    private static String sanitize(String value) {
        return (value + "").replace("\r", "_").replace("\n", "_");
    }
}
