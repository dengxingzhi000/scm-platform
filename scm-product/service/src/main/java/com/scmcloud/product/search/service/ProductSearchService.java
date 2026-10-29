package com.scmcloud.product.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.product.search.document.ProductDocument;
import com.scmcloud.product.search.dto.ProductSearchRequest;
import com.scmcloud.product.search.dto.ProductSearchResponse;
import com.scmcloud.product.search.repository.ProductSearchRepository;

/**
 * 商品搜索服务
 *
 * <p>提供商品全文搜索、分类筛选、品牌筛选、价格区间查询等功能
 *
 * <p>性能优化
 * - 热门商品列表缓存 5 分钟
 * - 最新商品列表缓5 分钟
 * - 使用 Redis 缓存热门搜索
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ProductSearchRepository productSearchRepository;

    /**
     * 商品状态：上架
     */
    private static final Integer STATUS_ON_SALE = 1;

    /**
     * 综合搜索（支持多条件组合
     *
     * @param request 搜索请求
     * @return 商品分页结果
     */
    public Page<ProductSearchResponse> search(ProductSearchRequest request) {
        long startTime = System.currentTimeMillis();

        log.debug("🔍 [商品搜索] 开始搜索 keyword={}, categoryId={}, brandId={}, priceRange=[{},{}], sortBy={}, sortOrder={}",
                (request.getKeyword() + "").replace("\r", "_").replace("\n", "_"), (request.getCategoryId() + "").replace("\r", "_").replace("\n", "_"), (request.getBrandId() + "").replace("\r", "_").replace("\n", "_"),
                (request.getMinPrice() + "").replace("\r", "_").replace("\n", "_"), (request.getMaxPrice() + "").replace("\r", "_").replace("\n", "_"), (request.getSortBy() + "").replace("\r", "_").replace("\n", "_"), (request.getSortOrder() + "").replace("\r", "_").replace("\n", "_"));

        // 构建分页和排序
        PageRequest pageRequest = buildPageRequest(request);

        // 执行搜索
        Page<ProductDocument> page;
        if (hasAdvancedFilters(request)) {
            // 高级搜索（多条件组合
            page = advancedSearch(request, pageRequest);
        } else if (StringUtils.hasText(request.getKeyword())) {
            // 全文搜索
            page = productSearchRepository.fullTextSearch(request.getKeyword(), STATUS_ON_SALE, pageRequest);
        } else {
            // 默认查询（按销量排序）
            page = productSearchRepository.findByStatusOrderByTotalSalesDesc(STATUS_ON_SALE, pageRequest);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.debug("✅[商品搜索] 搜索完成: 总数={}, 页码={}/{}, 耗时={}ms",
                page.getTotalElements(), page.getNumber() + 1, page.getTotalPages(), duration);

        // 转换DTO
        return page.map(this::convertToResponse);
    }

    /**
     * 热门商品（按销量排序）
     *
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 商品分页结果
     */
    @Cacheable(value = "hotProducts", key = "#page + '_' + #size", unless = "#result == null")
    public Page<ProductSearchResponse> getHotProducts(Integer page, Integer size) {
        log.debug("🔥 [热门商品] 查询热门商品: page={}, size={}", page, size);

        PageRequest pageRequest = toPageRequest(page, size, "totalSales");
        Page<ProductDocument> resultPage = productSearchRepository.findByStatusOrderByTotalSalesDesc(STATUS_ON_SALE, pageRequest);

        log.debug("✅[热门商品] 查询完成: 总数={}", resultPage.getTotalElements());
        return resultPage.map(this::convertToResponse);
    }

    /**
     * 最新商品（按发布时间排序）
     *
     * @param page 页码（从 1 开始）
     * @param size 每页数量
     * @return 商品分页结果
     */
    @Cacheable(value = "latestProducts", key = "#page + '_' + #size", unless = "#result == null")
    public Page<ProductSearchResponse> getLatestProducts(Integer page, Integer size) {
        log.debug("🆕 [最新商品] 查询最新商品 page={}, size={}", page, size);

        PageRequest pageRequest = toPageRequest(page, size, "publishedAt");
        Page<ProductDocument> resultPage = productSearchRepository.findByStatusOrderByPublishedAtDesc(STATUS_ON_SALE, pageRequest);

        log.debug("✅[最新商品] 查询完成: 总数={}", resultPage.getTotalElements());
        return resultPage.map(this::convertToResponse);
    }

    /**
     * 按分类查询商品
     *
     * @param categoryId 分类 ID
     * @param page       页码（从 1 开始）
     * @param size       每页数量
     * @return 商品分页结果
     */
    public Page<ProductSearchResponse> findByCategory(String categoryId, Integer page, Integer size) {
        log.debug("📁 [分类商品] 查询分类商品: categoryId={}, page={}, size={}", (categoryId + "").replace("\r", "_").replace("\n", "_"), (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"));

        PageRequest pageRequest = toPageRequest(page, size, "totalSales");
        Page<ProductDocument> resultPage = productSearchRepository.findByCategoryIdAndStatus(categoryId, STATUS_ON_SALE, pageRequest);

        log.debug("✅[分类商品] 查询完成: 总数={}", resultPage.getTotalElements());
        return resultPage.map(this::convertToResponse);
    }

    /**
     * 按品牌查询商品
     *
     * @param brandId 品牌 ID
     * @param page    页码（从 1 开始）
     * @param size    每页数量
     * @return 商品分页结果
     */
    public Page<ProductSearchResponse> findByBrand(String brandId, Integer page, Integer size) {
        log.info("🏷[品牌商品] 查询品牌商品: brandId={}, page={}, size={}", (brandId + "").replace("\r", "_").replace("\n", "_"), (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"));

        PageRequest pageRequest = toPageRequest(page, size, "totalSales");
        Page<ProductDocument> resultPage = productSearchRepository.findByBrandIdAndStatus(brandId, STATUS_ON_SALE, pageRequest);

        log.debug("✅[品牌商品] 查询完成: 总数={}", resultPage.getTotalElements());
        return resultPage.map(this::convertToResponse);
    }

    // ==================== 私有方法 ====================

    /**
     * 高级搜索（支持多条件组合
     */
    private Page<ProductDocument> advancedSearch(ProductSearchRequest request, PageRequest pageRequest) {
        // 如果有高级过滤条件，使用 advancedSearch 方法
        return productSearchRepository.advancedSearch(
                StringUtils.hasText(request.getKeyword()) ? request.getKeyword() : "*",
                request.getCategoryId(),
                request.getBrandId(),
                request.getMinPrice(),
                request.getMaxPrice(),
                STATUS_ON_SALE,
                pageRequest
        );
    }

    /**
     * 构建分页请求（页码从 1 开始，越界值收敛到合法区间）
     */
    private PageRequest toPageRequest(Integer page, Integer size, String sortField) {
        int pageIndex = Math.max(1, page == null ? 1 : page) - 1;
        int pageSize = Math.max(1, Math.min(size == null ? 20 : size, 100));
        return PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, sortField));
    }

    /**
     * 构建分页请求
     */
    private PageRequest buildPageRequest(ProductSearchRequest request) {        // 页码1 开始，转换为从 0 开
        int page = Math.max(request.getPage() - 1, 0);
        // 每页数量限制：最1，最100
        int size = Math.max(1, Math.min(request.getSize(), 100));

        Sort sort = buildSort(request.getSortBy(), request.getSortOrder());
        return PageRequest.of(page, size, sort);
    }

    /**
     * 构建排序
     */
    private Sort buildSort(String sortBy, String sortOrder) {
        // 默认降序
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        // 根据 sortBy 字段选择排序字段
        return switch (sortBy != null ? sortBy.toLowerCase() : "sales") {
            case "price" -> Sort.by(direction, "minPrice");
            case "time" -> Sort.by(direction, "publishedAt");
            case "update" -> Sort.by(direction, "updateTime");
            case "sales" -> Sort.by(direction, "totalSales");
            default -> Sort.by(Sort.Direction.DESC, "totalSales");
        };
    }

    /**
     * 判断是否有高级过滤条
     */
    private boolean hasAdvancedFilters(ProductSearchRequest request) {
        return StringUtils.hasText(request.getCategoryId()) ||
               StringUtils.hasText(request.getBrandId()) ||
               request.getMinPrice() != null ||
               request.getMaxPrice() != null;
    }

    /**
     * 转换为响DTO
     */
    private ProductSearchResponse convertToResponse(ProductDocument document) {
        ProductSearchResponse response = new ProductSearchResponse();
        BeanUtils.copyProperties(document, response);
        return response;
    }
}