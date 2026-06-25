package com.scmcloud.search.service.impl;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.repository.ProductDocumentRepository;
import com.scmcloud.search.service.IProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductSearchServiceImpl implements IProductSearchService {

    private final ProductDocumentRepository productRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public List<ProductDocument> searchProducts(String keyword, Long categoryId, Long brandId, int page, int size) {
        log.info("Searching products: keyword={}, categoryId={}, brandId={}", keyword, categoryId, brandId);

        Criteria criteria = new Criteria("status").is("active");

        if (keyword != null && !keyword.isEmpty()) {
            criteria = criteria.and(new Criteria("title").matches(keyword))
                    .or(new Criteria("description").matches(keyword));
        }

        if (categoryId != null) {
            criteria = criteria.and(new Criteria("categoryId").is(categoryId));
        }

        if (brandId != null) {
            criteria = criteria.and(new Criteria("brandId").is(brandId));
        }

        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(page, size));

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        return hits.getSearchHits().stream()
                .map(hit -> hit.getContent())
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDocument> getSimilarProducts(Long productId, int limit) {
        log.info("Getting similar products: productId={}, limit={}", productId, limit);
        return productRepository.findByStatus("active")
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDocument> getPersonalizedResults(Long userId, int limit) {
        log.info("Getting personalized results: userId={}, limit={}", userId, limit);
        return productRepository.findByStatus("active")
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void indexProduct(ProductDocument product) {
        log.info("Indexing product: productId={}", product.getProductId());
        productRepository.save(product);
    }

    @Override
    public void removeProduct(Long productId) {
        log.info("Removing product from index: productId={}", productId);
        productRepository.findByStatus("active")
                .stream()
                .filter(p -> p.getProductId().equals(productId))
                .findFirst()
                .ifPresent(productRepository::delete);
    }

    @Override
    public void reindexAll() {
        log.info("Reindexing all products");
    }
}
