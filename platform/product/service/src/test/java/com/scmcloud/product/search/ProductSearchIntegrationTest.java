package com.scmcloud.product.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Tag;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import com.scmcloud.product.search.document.ProductDocument;
import com.scmcloud.product.search.dto.ProductSearchRequest;
import com.scmcloud.product.search.dto.ProductSearchResponse;
import com.scmcloud.product.search.repository.ProductSearchRepository;
import com.scmcloud.product.search.service.ProductSearchService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@RequiredArgsConstructor
@Slf4j
@SpringBootTest
@Tag("integration")
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Product Search Integration Test")
public class ProductSearchIntegrationTest {

    private final ProductSearchService productSearchService;

    private final ProductSearchRepository productSearchRepository;

    private static final String TEST_CATEGORY_ID = "cat_test_001";
    private static final String TEST_BRAND_ID = "brand_test_001";

    @BeforeEach
    public void setup() {
        log.info("=== Preparing product search test data ===");

        productSearchRepository.deleteAll();

        createTestProduct("prod_001", "iPhone 15 Pro Max", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("9999.00"), new BigDecimal("13999.00"), 1000, 5000, 100);

        createTestProduct("prod_002", "iPhone 15 Pro", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("7999.00"), new BigDecimal("9999.00"), 800, 4000, 90);

        createTestProduct("prod_003", "MacBook Pro 16", TEST_CATEGORY_ID, "brand_test_002",
                new BigDecimal("19999.00"), new BigDecimal("29999.00"), 500, 2000, 80);

        createTestProduct("prod_004", "AirPods Pro", "cat_test_002", TEST_BRAND_ID,
                new BigDecimal("1999.00"), new BigDecimal("1999.00"), 2000, 10000, 70);

        createTestProduct("prod_005", "iPad Pro", TEST_CATEGORY_ID, TEST_BRAND_ID,
                new BigDecimal("6999.00"), new BigDecimal("15999.00"), 600, 3000, 60);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Test data ready: inserted 5 products");
    }

    @Test
    @Order(1)
    @DisplayName("Scenario 1: Full text search by keyword")
    public void testFullTextSearch_Keyword() {
        log.info("=== Scenario 1: Full text search - iPhone keyword ===");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("iPhone");
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "Search result should not be null");
        assertTrue(result.getTotalElements() >= 2, "Should find at least 2 iPhone products");

        log.info("Full text search passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 1 passed ===");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario 2: Category filter")
    public void testCategoryFilter() {
        log.info("=== Scenario 2: Category filter ===");

        Page<ProductSearchResponse> result = productSearchService.findByCategory(TEST_CATEGORY_ID, 1, 20);

        assertNotNull(result, "Search result should not be null");
        assertEquals(4, result.getTotalElements(), "Should find 4 products in same category");

        result.getContent().forEach(product -> {
            assertEquals(TEST_CATEGORY_ID, product.getCategoryId(), "Product category should match");
        });

        log.info("Category filter passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 2 passed ===");
    }

    @Test
    @Order(3)
    @DisplayName("Scenario 3: Brand filter")
    public void testBrandFilter() {
        log.info("=== Scenario 3: Brand filter ===");

        Page<ProductSearchResponse> result = productSearchService.findByBrand(TEST_BRAND_ID, 1, 20);

        assertNotNull(result, "Search result should not be null");
        assertEquals(4, result.getTotalElements(), "Should find 4 products in same brand");

        result.getContent().forEach(product -> {
            assertEquals(TEST_BRAND_ID, product.getBrandId(), "Product brand should match");
        });

        log.info("Brand filter passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 3 passed ===");
    }

    @Test
    @Order(4)
    @DisplayName("Scenario 4: Price range filter")
    public void testPriceRangeFilter() {
        log.info("=== Scenario 4: Price range filter 5000-10000 ===");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setMinPrice(new BigDecimal("5000.00"));
        request.setMaxPrice(new BigDecimal("10000.00"));
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "Search result should not be null");
        assertTrue(result.getTotalElements() >= 1, "Should find at least 1 product");

        log.info("Price range filter passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 4 passed ===");
    }

    @Test
    @Order(5)
    @DisplayName("Scenario 5: Advanced search (keyword + category + brand)")
    public void testAdvancedSearch() {
        log.info("=== Scenario 5: Advanced search ===");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setKeyword("iPhone");
        request.setCategoryId(TEST_CATEGORY_ID);
        request.setBrandId(TEST_BRAND_ID);
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "Search result should not be null");
        assertTrue(result.getTotalElements() >= 2, "Should find at least 2 products");

        log.info("Advanced search passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 5 passed ===");
    }

    @Test
    @Order(6)
    @DisplayName("Scenario 6: Sort by sales descending")
    public void testSortBySales() {
        log.info("=== Scenario 6: Sort by sales descending ===");

        ProductSearchRequest request = new ProductSearchRequest();
        request.setSortBy("sales");
        request.setSortOrder("desc");
        request.setPage(1);
        request.setSize(20);

        Page<ProductSearchResponse> result = productSearchService.search(request);

        assertNotNull(result, "Search result should not be null");
        assertTrue(result.getTotalElements() >= 5, "Should find 5 products");

        ProductSearchResponse first = result.getContent().get(0);
        assertEquals(10000, first.getTotalSales(), "First product should have highest sales (10000)");

        log.info("Sort by sales passed: first product sales={}", first.getTotalSales());

        log.info("=== Scenario 6 passed ===");
    }

    @Test
    @Order(7)
    @DisplayName("Scenario 7: Hot products list")
    public void testHotProducts() {
        log.info("=== Scenario 7: Hot products list ===");

        Page<ProductSearchResponse> result = productSearchService.getHotProducts(1, 10);

        assertNotNull(result, "Hot products list should not be null");
        assertTrue(result.getTotalElements() >= 5, "Should find 5 products");

        log.info("Hot products list passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 7 passed ===");
    }

    @Test
    @Order(8)
    @DisplayName("Scenario 8: Latest products list")
    public void testLatestProducts() {
        log.info("=== Scenario 8: Latest products list ===");

        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(1, 10);

        assertNotNull(result, "Latest products list should not be null");
        assertTrue(result.getTotalElements() >= 5, "Should find 5 products");

        log.info("Latest products list passed: found {} products", result.getTotalElements());

        log.info("=== Scenario 8 passed ===");
    }

    @AfterEach
    public void cleanup() {
        log.info("Cleaning up product search test data...");
        productSearchRepository.deleteAll();
        log.info("Test data cleaned up");
    }

    private void createTestProduct(String id, String spuName, String categoryId, String brandId,
                                     BigDecimal minPrice, BigDecimal maxPrice,
                                     Integer totalStock, Integer totalSales, Integer sortOrder) {
        ProductDocument product = new ProductDocument();
        product.setId(id);
        product.setSpuCode("SPU_" + id);
        product.setSpuName(spuName);
        product.setCategoryId(categoryId);
        product.setCategoryName("Test Category");
        product.setBrandId(brandId);
        product.setBrandName("Test Brand");
        product.setDescription(spuName + " description");
        product.setMainImage("https://example.com/images/" + id + ".jpg");
        product.setMinPrice(minPrice);
        product.setMaxPrice(maxPrice);
        product.setTotalStock(totalStock);
        product.setTotalSales(totalSales);
        product.setSortOrder(sortOrder);
        product.setStatus(1);
        product.setPublishedAt(LocalDateTime.now());
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        productSearchRepository.save(product);
    }
}
