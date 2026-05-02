package org.ecommerce.api.graphql;

import org.ecommerce.api.dto.PagedResponse;
import org.ecommerce.api.entity.OrderEntity;
import org.ecommerce.api.entity.PaymentEntity;
import org.ecommerce.api.graphql.input.PaymentFilter;
import org.ecommerce.api.graphql.input.PaymentInput;
import org.ecommerce.api.repository.OrderRepository;
import org.ecommerce.api.service.PaymentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

/**
 * GraphQL resolver for Payment queries, mutations, and nested field resolvers.
 *
 * <p>Because OSIV is disabled, the {@code Payment.order} resolver loads the
 * OrderEntity explicitly rather than traversing the lazy association. Order
 * sub-fields (user, items) are then handled by {@link OrderGraphQlController}.
 */
@Controller
@Transactional(readOnly = true)
public class PaymentGraphQlController {

    private final PaymentService  paymentService;
    private final OrderRepository orderRepository;

    public PaymentGraphQlController(PaymentService paymentService,
                                    OrderRepository orderRepository) {
        this.paymentService  = paymentService;
        this.orderRepository = orderRepository;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @QueryMapping
    public PaymentEntity payment(@Argument Long id) {
        return paymentService.findById(id);
    }

    @QueryMapping
    public PagedResponse<PaymentEntity> payments(
            @Argument PaymentFilter filter,
            @Argument int page,
            @Argument int size) {

        Long   orderId = filter != null ? filter.getOrderId() : null;
        String status  = filter != null ? filter.getStatus()  : null;

        return paymentService.findAll(orderId, status, PageRequest.of(page, size));
    }

    // ── Nested field resolvers ─────────────────────────────────────────────────

    @SchemaMapping(typeName = "Payment", field = "order")
    public OrderEntity order(PaymentEntity payment) {
        Long oid = payment.getOrderId();
        return oid != null ? orderRepository.findById(oid).orElse(null) : null;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    @MutationMapping
    @Transactional
    public PaymentEntity createPayment(@Argument PaymentInput input) {
        return paymentService.create(input.toRequest());
    }

    @MutationMapping
    @Transactional
    public PaymentEntity updatePaymentStatus(@Argument Long id, @Argument String status) {
        return paymentService.updateStatus(id, status);
    }
}
