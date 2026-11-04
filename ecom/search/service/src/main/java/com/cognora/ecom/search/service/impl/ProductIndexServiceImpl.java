package com.cognora.ecom.search.service.impl;

import com.cognora.ecom.search.domain.ProductDocument;
import com.cognora.ecom.search.repository.ProductDocumentRepository;
import com.cognora.ecom.search.service.IProductIndexService;
import com.cognora.ecom.search.tenant.SearchTenantResolver;
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
    private final SearchTenantResolver tenantResolver;

    @Override
    public int bulkIndex(List<ProductDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return 0;
        }
        String tenantId = tenantResolver.resolveTenantIdString();
        documents.forEach(document -> ProductSearchServiceImpl.applyTenant(document, tenantId));
        productRepository.saveAll(documents);
        log.info("Bulk indexed {} product documents for tenant {}", documents.size(), tenantId);
        return documents.size();
    }

    @Override
    public int reindexAll(String tenantId) {
        List<ProductDocument> documents = new ArrayList<>();
        productRepository.findByTenantId(tenantId).forEach(documents::add);
        if (documents.isEmpty()) {
            log.info("Reindex skipped: no documents for tenant {}", tenantId);
            return 0;
        }
        documents.forEach(document -> ProductSearchServiceImpl.applyTenant(document, tenantId));
        productRepository.saveAll(documents);
        log.info("Reindexed {} product documents for tenant {}", documents.size(), tenantId);
        return documents.size();
    }

    @Override
    public void deleteProduct(String tenantId, Long productId) {
        List<ProductDocument> documents = productRepository.findByTenantIdAndProductId(tenantId, productId);
        if (!documents.isEmpty()) {
            productRepository.deleteAll(documents);
            log.info("Deleted {} index documents for tenant {} and product {}",
                    documents.size(), tenantId, productId);
        }
    }
}
