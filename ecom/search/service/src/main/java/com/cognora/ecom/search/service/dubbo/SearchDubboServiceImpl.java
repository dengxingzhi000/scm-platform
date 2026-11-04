package com.cognora.ecom.search.service.dubbo;

import com.cognora.ecom.search.api.SearchDubboService;
import com.cognora.ecom.search.api.dto.ProductSearchItem;
import com.cognora.ecom.search.domain.ProductDocument;
import com.cognora.ecom.search.service.IProductIndexService;
import com.cognora.ecom.search.service.IProductSearchService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.UUID;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class SearchDubboServiceImpl implements SearchDubboService {

    private final IProductSearchService productSearchService;
    private final IProductIndexService productIndexService;

    @Override
    public List<ProductSearchItem> searchProducts(String keyword, Long categoryId, Long brandId,
                                                  int page, int size, String tenantId) {
        UUID tenant = requireTenant(tenantId);
        return TenantContextHolder.executeInTenantContext(tenant,
                        () -> productSearchService.searchProducts(keyword, categoryId, brandId, page, size))
                .stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    public void indexProduct(ProductSearchItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Product search item must not be null");
        }
        UUID tenant = requireTenant(item.getTenantId());
        TenantContextHolder.executeInTenantContext(tenant, () -> {
            productSearchService.indexProduct(toDocument(item));
            return null;
        });
    }

    @Override
    public void removeProduct(Long productId, String tenantId) {
        UUID tenant = requireTenant(tenantId);
        TenantContextHolder.executeInTenantContext(tenant, () -> {
            productIndexService.deleteProduct(tenantId, productId);
            return null;
        });
    }

    @Override
    public int reindexAll(String tenantId) {
        UUID tenant = requireTenant(tenantId);
        return TenantContextHolder.executeInTenantContext(tenant,
                () -> productIndexService.reindexAll(tenantId));
    }

    static UUID requireTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required for search operations");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("tenantId must be a UUID", exception);
        }
    }

    private ProductDocument toDocument(ProductSearchItem item) {
        ProductDocument document = new ProductDocument();
        document.setId(item.getId());
        document.setTenantId(item.getTenantId());
        document.setProductId(item.getProductId());
        document.setSellerId(item.getSellerId());
        document.setTitle(item.getTitle());
        document.setSubtitle(item.getSubtitle());
        document.setDescription(item.getDescription());
        document.setCategoryId(item.getCategoryId());
        document.setBrandId(item.getBrandId());
        document.setBrandName(item.getBrandName());
        document.setPrice(item.getPrice());
        document.setOriginalPrice(item.getOriginalPrice());
        document.setSalesCount(item.getSalesCount());
        document.setRating(item.getRating());
        document.setReviewCount(item.getReviewCount());
        document.setMainImage(item.getMainImage());
        document.setStatus(item.getStatus());
        return document;
    }

    private ProductSearchItem toItem(ProductDocument document) {
        ProductSearchItem item = new ProductSearchItem();
        item.setId(document.getId());
        item.setTenantId(document.getTenantId());
        item.setProductId(document.getProductId());
        item.setSellerId(document.getSellerId());
        item.setTitle(document.getTitle());
        item.setSubtitle(document.getSubtitle());
        item.setDescription(document.getDescription());
        item.setCategoryId(document.getCategoryId());
        item.setBrandId(document.getBrandId());
        item.setBrandName(document.getBrandName());
        item.setPrice(document.getPrice());
        item.setOriginalPrice(document.getOriginalPrice());
        item.setSalesCount(document.getSalesCount());
        item.setRating(document.getRating());
        item.setReviewCount(document.getReviewCount());
        item.setMainImage(document.getMainImage());
        item.setStatus(document.getStatus());
        return item;
    }
}
