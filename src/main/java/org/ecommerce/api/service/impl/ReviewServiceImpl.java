package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.ReviewRequest;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.ProductEntity;
import org.ecommerce.api.entity.ReviewEntity;
import org.ecommerce.api.entity.UserEntity;
import org.ecommerce.api.repository.OrderRepository;
import org.ecommerce.api.repository.ProductRepository;
import org.ecommerce.api.repository.ReviewRepository;
import org.ecommerce.api.repository.UserRepository;
import org.ecommerce.api.service.ReviewService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository  reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository    userRepository;
    private final OrderRepository   orderRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             ProductRepository productRepository,
                             UserRepository userRepository,
                             OrderRepository orderRepository) {
        this.reviewRepository  = reviewRepository;
        this.productRepository = productRepository;
        this.userRepository    = userRepository;
        this.orderRepository   = orderRepository;
    }

    @Override
    public PagedResponse<ReviewEntity> findAll(Long productId, Boolean approved, Pageable pageable) {
        return PagedResponse.of(reviewRepository.search(productId, approved, pageable));
    }

    @Override
    public ReviewEntity findById(long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Review not found with id: " + id));
    }

    @Override
    @Transactional
    public ReviewEntity create(ReviewRequest request) {
        ProductEntity product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found with id: " + request.getProductId()));

        UserEntity user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found with id: " + request.getUserId()));

        if (reviewRepository.existsByProduct_ProductIdAndUser_UserId(
                request.getProductId(), request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User has already reviewed this product");
        }

        ReviewEntity review = new ReviewEntity();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle());
        review.setBody(request.getBody());

        if (request.getOrderId() != null) {
            OrderEntity order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Order not found with id: " + request.getOrderId()));
            review.setOrder(order);
        }

        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public ReviewEntity approve(long id) {
        ReviewEntity review = findById(id);
        if (review.isApproved()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Review is already approved");
        }
        review.setApproved(true);
        return reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void delete(long id) {
        reviewRepository.delete(findById(id));
    }
}
