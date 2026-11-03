package com.cognora.ecom.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.cognora.ecom.mall.domain.entity.Review;

public interface IReviewService extends IService<Review> {

    Review createReview(Review review);

    Page<Review> listByProduct(Long productId, int page, int size);

    void deleteReview(Long reviewId, String userId);
}
