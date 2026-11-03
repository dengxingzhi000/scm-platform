package com.scmcloud.search.service.dubbo;

import com.scmcloud.search.api.SearchDubboService;
import com.scmcloud.search.api.dto.ProductSearchItem;
import com.scmcloud.search.domain.ProductDocument;
import com.scmcloud.search.service.IProductIndexService;
import com.scmcloud.search.service.IProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@Slf4j
@DubboService
@RequiredArgsConstructor
public class SearchDubboServiceImpl implements SearchDubboService {

    private final IProductSearchService productSearchService;
    private final IProductIndexService productIndexService;

    @Override
    public List<ProductSearchItem> searchProducts(String keyword, Long categoryId, Long brandId,
                                                  int page, int size) {
        return productSearchService.searchProducts(keyword, categoryId, brandId, page, size).stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    public void indexProduct(ProductSearchItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Product search item must not be null");
        }
        productSearchService.indexProduct(toDocument(item));
    }

    @Override
    public void removeProduct(Long productId) {
        productIndexService.deleteProduct(productId);
    }

    @Override
    public int reindexAll() {
        return productIndexService.reindexAll();
    }

    private ProductDocument toDocument(ProductSearchItem item) {
        ProductDocument document = new ProductDocument();
        document.setId(item.getId());
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
