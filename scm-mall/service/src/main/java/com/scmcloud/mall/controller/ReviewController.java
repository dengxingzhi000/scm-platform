package com.scmcloud.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.mall.domain.entity.Review;
import com.scmcloud.mall.service.IReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/mall")
@Tag(name = "商品评价", description = "商品评价的发布、按商品分页查询与删除接口。")
public class ReviewController {

    private final IReviewService reviewService;

    @Operation(summary = "发表商品评价", description = "为指定商品创建一条用户评价并返回评价结果。")
    @PostMapping("/reviews")
    public Review createReview(@RequestBody Review review) {
        log.info("[API] Create review: productId={}, userId={}",
                review.getProductId(), review.getUserId());
        return reviewService.createReview(review);
    }

    @Operation(summary = "分页查询商品评价", description = "按商品 ID 分页获取该商品的用户评价列表。")
    @GetMapping("/products/{productId}/reviews")
    public Page<Review> listReviews(@Parameter(description = "商品 ID") @PathVariable Long productId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return reviewService.listByProduct(productId, page, size);
    }

    @Operation(summary = "删除商品评价", description = "由发表评价的用户本人删除指定评价。")
    @DeleteMapping("/reviews/{reviewId}")
    public boolean deleteReview(@Parameter(description = "评价 ID") @PathVariable Long reviewId, @Parameter(description = "删除操作的用户标识") @RequestParam String userId) {
        log.info("[API] Delete review: reviewId={}, userId={}", reviewId, userId);
        reviewService.deleteReview(reviewId, userId);
        return true;
    }
}
