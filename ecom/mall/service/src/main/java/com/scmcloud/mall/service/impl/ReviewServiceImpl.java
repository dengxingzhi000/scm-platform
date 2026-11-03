package com.scmcloud.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.mall.domain.entity.Review;
import com.scmcloud.mall.mapper.ReviewMapper;
import com.scmcloud.mall.service.IReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Slf4j
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements IReviewService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Review createReview(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("review must not be null");
        }
        if (review.getUserId() == null || review.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        if (review.getProductId() == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (review.getRating() == null || review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        review.setStatus(1);
        review.setCreatedAt(LocalDateTime.now());
        save(review);
        log.info("Review created: reviewId={}, productId={}, userId={}, rating={}",
                (review.getId() + "").replace("\r", "_").replace("\n", "_"), (review.getProductId() + "").replace("\r", "_").replace("\n", "_"), (review.getUserId() + "").replace("\r", "_").replace("\n", "_"), (review.getRating() + "").replace("\r", "_").replace("\n", "_"));
        return review;
    }

    @Override
    public Page<Review> listByProduct(Long productId, int page, int size) {
        return page(new Page<>(Math.max(1, page), Math.min(Math.max(1, size), 100)),
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getProductId, productId)
                        .eq(Review::getStatus, 1)
                        .orderByDesc(Review::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long reviewId, String userId) {
        Review review = getById(reviewId);
        if (review == null) {
            throw new IllegalArgumentException("Review not found: " + reviewId);
        }
        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("Cannot delete another user's review");
        }
        removeById(reviewId);
        log.info("Review deleted: reviewId={}, userId={}", (reviewId + "").replace("\r", "_").replace("\n", "_"), (userId + "").replace("\r", "_").replace("\n", "_"));
    }
}
