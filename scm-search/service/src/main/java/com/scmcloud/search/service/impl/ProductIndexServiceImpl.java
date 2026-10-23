package com.scmcloud.search.service.impl;

import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.repository.ProductDocumentRepository;
import com.scmcloud.search.service.IProductIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class ProductIndexServiceImpl implements IProductIndexService {

    private final ProductDocumentRepository productRepository;

    @Override
    public int bulkIndex(List<ProductDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        productRepository.saveAll(documents);
        log.info("Bulk indexed {} product documents", documents.size());
        return documents.size();
    }

    @Override
    public int reindexAll() {
        List<ProductDocument> documents = new ArrayList<>();
        productRepository.findAll().forEach(documents::add);
        if (documents.isEmpty()) {
            log.info("Reindex skipped: index is empty");
            return 0;
        }
        productRepository.saveAll(documents);
        log.info("Reindexed {} product documents", documents.size());
        return documents.size();
    }

    @Override
    public void deleteProduct(Long productId) {
        List<ProductDocument> documents = productRepository.findByProductId(productId);
        if (!documents.isEmpty()) {
            productRepository.deleteAll(documents);
            log.info("Deleted {} index documents for product {}", documents.size(), productId);
        }
    }
}
