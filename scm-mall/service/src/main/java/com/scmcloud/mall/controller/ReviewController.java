package com.scmcloud.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.mall.domain.entity.Review;
import com.scmcloud.mall.service.IReviewService;
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
public class ReviewController {

    private final IReviewService reviewService;

    @PostMapping("/reviews")
    public Review createReview(@RequestBody Review review) {
        log.info("[API] Create review: productId={}, userId={}",
                review.getProductId(), review.getUserId());
        return reviewService.createReview(review);
    }

    @GetMapping("/products/{productId}/reviews")
    public Page<Review> listReviews(@PathVariable Long productId,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return reviewService.listByProduct(productId, page, size);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public boolean deleteReview(@PathVariable Long reviewId, @RequestParam String userId) {
        log.info("[API] Delete review: reviewId={}, userId={}", reviewId, userId);
        reviewService.deleteReview(reviewId, userId);
        return true;
    }
}
