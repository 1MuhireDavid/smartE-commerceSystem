package org.ecommerce.api.service.impl;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.dto.request.PaymentRequest;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.PaymentEntity;
import org.ecommerce.api.repository.OrderRepository;
import org.ecommerce.api.repository.PaymentRepository;
import org.ecommerce.api.service.PaymentService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> VALID_STATUSES =
            Set.of("pending", "completed", "failed", "refunded");

    private final PaymentRepository paymentRepository;
    private final OrderRepository   orderRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository   = orderRepository;
    }

    @Override
    public PagedResponse<PaymentEntity> findAll(Long orderId, String status, Pageable pageable) {
        return PagedResponse.of(paymentRepository.search(orderId, status, pageable));
    }

    @Override
    public PaymentEntity findById(long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found with id: " + id));
    }

    @Override
    @Transactional
    public PaymentEntity create(PaymentRequest request) {
        OrderEntity order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Order not found with id: " + request.getOrderId()));

        PaymentEntity payment = new PaymentEntity();
        payment.setOrder(order);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionId(request.getTransactionId());
        payment.setAmount(request.getAmount());
        return paymentRepository.save(payment);
    }

    @Override
    @Transactional
    public PaymentEntity updateStatus(long id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status '" + status + "'. Allowed: " + VALID_STATUSES);
        }

        PaymentEntity payment = findById(id);
        payment.setStatus(status);
        if ("completed".equals(status)) {
            payment.setPaidAt(LocalDateTime.now());
        }
        return paymentRepository.save(payment);
    }
}
