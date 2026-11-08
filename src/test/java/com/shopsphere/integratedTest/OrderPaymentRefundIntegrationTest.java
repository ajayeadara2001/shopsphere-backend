package com.shopsphere.integratedTest;

import com.shopsphere.constant.OrderStatus;
import com.shopsphere.constant.PaymentMethod;
import com.shopsphere.constant.PaymentStatus;
import com.shopsphere.constant.RefundStatus;
import com.shopsphere.constant.Role;
import com.shopsphere.dto.OrderStatusUpdateRequestDto;
import com.shopsphere.dto.PaymentRequestDto;
import com.shopsphere.dto.RefundRequestDto;
import com.shopsphere.dto.RefundStatusRequestDto;
import com.shopsphere.entity.*;
import com.shopsphere.repository.*;
import com.shopsphere.service.OrderService;
import com.shopsphere.service.PaymentService;
import com.shopsphere.service.RefundService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class OrderPaymentRefundIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemsRepository cartItemsRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private OrderService orderService;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @WithMockUser(
            username = "integration.customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void fullPaymentRefundAndCancellationLifecycle_ShouldPersistConsistentStates() {
        User customer = User.builder()
                .userName("Integration Customer")
                .email("integration.customer@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("9999999999")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Order order = Order.builder()
                .orderNumber("ORD-INTEGRATION-001")
                .user(customer)
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .orderItemsList(new ArrayList<>())
                .discountAmount(BigDecimal.ZERO)
                .build();
        order = orderRepository.save(order);

        paymentService.createPayment(
                order.getOrderId(),
                PaymentRequestDto.builder()
                        .paymentMethod(PaymentMethod.UPI)
                        .build()
        );

        Payment payment = paymentRepository
                .findFirstByOrderOrderByPaymentIdDesc(order)
                .orElseThrow();

        assertEquals(PaymentStatus.PENDING, payment.getPaymentStatus());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(payment.getAmount()));

        paymentService.successPayment(payment.getPaymentId());

        Payment successfulPayment = paymentRepository
                .findById(payment.getPaymentId())
                .orElseThrow();
        Order confirmedOrder = orderRepository
                .findById(order.getOrderId())
                .orElseThrow();

        assertEquals(PaymentStatus.SUCCESS, successfulPayment.getPaymentStatus());
        assertNotNull(successfulPayment.getPaidAt());
        assertEquals(OrderStatus.CONFIRMED, confirmedOrder.getOrderStatus());

        refundService.applyRefund(new RefundRequestDto(
                successfulPayment.getPaymentId(),
                new BigDecimal("500.00"),
                "Integration lifecycle refund"
        ));

        Refund refund = refundRepository
                .findByPaymentPaymentId(successfulPayment.getPaymentId())
                .getFirst();

        assertEquals(RefundStatus.PENDING, refund.getRefundStatus());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(refund.getRefundAmount()));

        refundService.updateRefund(
                refund.getRefundId(),
                new RefundStatusRequestDto(RefundStatus.COMPLETED)
        );

        Payment refundedPayment = paymentRepository
                .findById(payment.getPaymentId())
                .orElseThrow();

        assertEquals(PaymentStatus.REFUNDED, refundedPayment.getPaymentStatus());

        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build()
        );

        Order cancelledOrder = orderRepository
                .findById(order.getOrderId())
                .orElseThrow();

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
        assertEquals(PaymentStatus.REFUNDED, refundedPayment.getPaymentStatus());
        assertEquals(1, refundRepository
                .findByPaymentPaymentId(payment.getPaymentId())
                .size());
    }

    @Test
    @WithMockUser(
            username = "stock.customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void cancelOrder_PlacedOrder_ShouldRestoreProductStockInDatabase() {
        User customer = User.builder()
                .userName("Stock Customer")
                .email("stock.customer@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("8888888888")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setCategoryName("Integration Category");
        category.setDescription("Category used for stock restoration testing");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Integration Product");
        product.setDescription("Product used for stock restoration testing");
        product.setPrice(new BigDecimal("250.00"));
        product.setStockQuantity(8);
        product.setBrand("ShopSphere");
        product.setCategory(category);
        product = productRepository.save(product);

        Order order = Order.builder()
                .orderNumber("ORD-STOCK-RESTORE-001")
                .user(customer)
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .discountAmount(BigDecimal.ZERO)
                .orderItemsList(new ArrayList<>())
                .build();

        OrderItems orderItem = OrderItems.builder()
                .order(order)
                .product(product)
                .quantity(2)
                .priceAtAdded(new BigDecimal("250.00"))
                .build();

        order.getOrderItemsList().add(orderItem);
        order = orderRepository.save(order);

        orderService.cancelOrder(order.getOrderId());

        entityManager.flush();
        entityManager.clear();

        Order cancelledOrder = orderRepository
                .findById(order.getOrderId())
                .orElseThrow();

        Product restoredProduct = productRepository
                .findById(product.getProductId())
                .orElseThrow();

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
        assertEquals(10, restoredProduct.getStockQuantity());
    }

    @Test
    @WithMockUser(
            username = "retry.customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void failedPayment_ShouldKeepOrderPlacedAndAllowNewPaymentAttempt() {
        User customer = User.builder()
                .userName("Retry Customer")
                .email("retry.customer@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("7777777777")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Order order = Order.builder()
                .orderNumber("ORD-PAYMENT-RETRY-001")
                .user(customer)
                .orderStatus(OrderStatus.PLACED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .discountAmount(BigDecimal.ZERO)
                .orderItemsList(new ArrayList<>())
                .build();
        order = orderRepository.save(order);

        paymentService.createPayment(
                order.getOrderId(),
                PaymentRequestDto.builder()
                        .paymentMethod(PaymentMethod.CARD)
                        .build()
        );

        Payment firstPayment = paymentRepository
                .findFirstByOrderOrderByPaymentIdDesc(order)
                .orElseThrow();

        paymentService.failedPayment(firstPayment.getPaymentId());

        entityManager.flush();
        entityManager.clear();

        Order orderAfterFailure = orderRepository
                .findById(order.getOrderId())
                .orElseThrow();

        Payment failedPayment = paymentRepository
                .findById(firstPayment.getPaymentId())
                .orElseThrow();

        assertEquals(OrderStatus.PLACED, orderAfterFailure.getOrderStatus());
        assertEquals(PaymentStatus.FAILED, failedPayment.getPaymentStatus());

        paymentService.createPayment(
                orderAfterFailure.getOrderId(),
                PaymentRequestDto.builder()
                        .paymentMethod(PaymentMethod.UPI)
                        .build()
        );

        Payment retryPayment = paymentRepository
                .findFirstByOrderOrderByPaymentIdDesc(orderAfterFailure)
                .orElseThrow();

        assertNotEquals(firstPayment.getPaymentId(), retryPayment.getPaymentId());
        assertEquals(PaymentStatus.PENDING, retryPayment.getPaymentStatus());
        assertEquals(PaymentMethod.UPI, retryPayment.getPaymentMethod());
        assertEquals(OrderStatus.PLACED, orderAfterFailure.getOrderStatus());
    }

    @Test
    @WithMockUser(
            username = "partial.refund@shopsphere.com",
            roles = "CUSTOMER"
    )
    void partialRefunds_ShouldMarkPaymentRefundedOnlyAfterFullAmountIsCompleted() {
        User customer = User.builder()
                .userName("Partial Refund Customer")
                .email("partial.refund@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("6666666666")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Order order = Order.builder()
                .orderNumber("ORD-PARTIAL-REFUND-001")
                .user(customer)
                .orderStatus(OrderStatus.CONFIRMED)
                .totalOrderAmount(new BigDecimal("500.00"))
                .discountAmount(BigDecimal.ZERO)
                .orderItemsList(new ArrayList<>())
                .build();
        order = orderRepository.save(order);

        Payment payment = Payment.builder()
                .paymentNumber("PAY-PARTIAL-REFUND-001")
                .order(order)
                .user(customer)
                .amount(new BigDecimal("500.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.UPI)
                .build();
        payment = paymentRepository.save(payment);

        refundService.applyRefund(new RefundRequestDto(
                payment.getPaymentId(),
                new BigDecimal("200.00"),
                "First partial refund"
        ));

        Refund firstRefund = refundRepository
                .findByPaymentPaymentId(payment.getPaymentId())
                .getFirst();

        refundService.updateRefund(
                firstRefund.getRefundId(),
                new RefundStatusRequestDto(RefundStatus.COMPLETED)
        );

        entityManager.flush();
        entityManager.clear();

        Payment paymentAfterFirstRefund = paymentRepository
                .findById(payment.getPaymentId())
                .orElseThrow();

        assertEquals(PaymentStatus.SUCCESS,
                paymentAfterFirstRefund.getPaymentStatus());

        refundService.applyRefund(new RefundRequestDto(
                payment.getPaymentId(),
                new BigDecimal("300.00"),
                "Second partial refund"
        ));

        List<Refund> refunds = refundRepository
                .findByPaymentPaymentId(payment.getPaymentId());

        Refund secondRefund = refunds.stream()
                .filter(refund -> !refund.getRefundId()
                        .equals(firstRefund.getRefundId()))
                .findFirst()
                .orElseThrow();

        refundService.updateRefund(
                secondRefund.getRefundId(),
                new RefundStatusRequestDto(RefundStatus.COMPLETED)
        );

        entityManager.flush();
        entityManager.clear();

        Payment fullyRefundedPayment = paymentRepository
                .findById(payment.getPaymentId())
                .orElseThrow();

        List<Refund> completedRefunds = refundRepository
                .findByPaymentPaymentId(payment.getPaymentId());
        assertEquals(PaymentStatus.REFUNDED,
                fullyRefundedPayment.getPaymentStatus());
        assertEquals(2, completedRefunds.size());
        assertTrue(completedRefunds.stream().allMatch(refund ->
                refund.getRefundStatus() == RefundStatus.COMPLETED));
    }

    @Test
    @WithMockUser(
            username = "place.order@shopsphere.com",
            roles = "CUSTOMER"
    )
    void placeOrder_ValidCartWithoutCoupon_ShouldReserveStockCreateOrderAndClearCart() {
        User customer = User.builder()
                .userName("Place Order Customer")
                .email("place.order@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("5555555555")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setCategoryName("Place Order Category");
        category.setDescription("Category for place-order integration test");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Place Order Product");
        product.setDescription("Product for place-order integration test");
        product.setPrice(new BigDecimal("250.00"));
        product.setStockQuantity(10);
        product.setBrand("ShopSphere");
        product.setCategory(category);
        product = productRepository.save(product);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>());

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(2);
        cartItem.setPriceAtAdded(new BigDecimal("250.00"));

        cart.getCartItemsList().add(cartItem);
        cartRepository.save(cart);

        orderService.placeOrder(null);

        entityManager.flush();
        entityManager.clear();

        User persistedCustomer = userRepository
                .findById(customer.getUserId())
                .orElseThrow();

        Product persistedProduct = productRepository
                .findById(product.getProductId())
                .orElseThrow();

        Cart clearedCart = cartRepository
                .findByUser(persistedCustomer)
                .orElseThrow();

        Order savedOrder = orderRepository
                .findByUser(persistedCustomer)
                .getFirst();

        assertEquals(8, persistedProduct.getStockQuantity());

        assertEquals(OrderStatus.PLACED, savedOrder.getOrderStatus());
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(savedOrder.getTotalOrderAmount()));
        assertEquals(1, savedOrder.getOrderItemsList().size());
        assertEquals(2, savedOrder.getOrderItemsList().getFirst().getQuantity());

        assertTrue(clearedCart.getCartItemsList().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(clearedCart.getTotalAmount()));
    }

    @Test
    @WithMockUser(
            username = "coupon.order@shopsphere.com",
            roles = "CUSTOMER"
    )
    void placeOrder_ValidCoupon_ShouldApplyDiscountAndIncrementCouponUsage() {
        User customer = User.builder()
                .userName("Coupon Customer")
                .email("coupon.order@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("4444444444")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setCategoryName("Coupon Order Category");
        category.setDescription("Category for coupon integration test");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Coupon Order Product");
        product.setDescription("Product for coupon integration test");
        product.setPrice(new BigDecimal("500.00"));
        product.setStockQuantity(10);
        product.setBrand("ShopSphere");
        product.setCategory(category);
        product = productRepository.save(product);

        Coupon coupon = new Coupon();
        coupon.setCouponCode("SAVE10");
        coupon.setActiveStatus(true);
        coupon.setExpiryDate(LocalDateTime.now().plusDays(1));
        coupon.setUsageCount(0);
        coupon.setUsageLimit(10);
        coupon.setDiscountPercentage(new BigDecimal("10.00"));
        coupon.setMinimumOrderAmount(new BigDecimal("100.00"));
        coupon = couponRepository.save(coupon);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("500.00"));
        cart.setCartItemsList(new ArrayList<>());

        CartItems cartItem = new CartItems();
        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItem.setQuantity(1);
        cartItem.setPriceAtAdded(new BigDecimal("500.00"));

        cart.getCartItemsList().add(cartItem);
        cartRepository.save(cart);

        orderService.placeOrder(" save10 ");

        entityManager.flush();
        entityManager.clear();

        User persistedCustomer = userRepository
                .findById(customer.getUserId())
                .orElseThrow();

        Coupon usedCoupon = couponRepository
                .findById(coupon.getCouponId())
                .orElseThrow();

        Product persistedProduct = productRepository
                .findById(product.getProductId())
                .orElseThrow();

        Cart clearedCart = cartRepository
                .findByUser(persistedCustomer)
                .orElseThrow();

        Order savedOrder = orderRepository
                .findByUser(persistedCustomer)
                .getFirst();

        assertEquals(1, usedCoupon.getUsageCount());
        assertEquals(9, persistedProduct.getStockQuantity());

        assertEquals("SAVE10", savedOrder.getCouponCode());
        assertEquals(0, new BigDecimal("50.00")
                .compareTo(savedOrder.getDiscountAmount()));
        assertEquals(0, new BigDecimal("450.00")
                .compareTo(savedOrder.getTotalOrderAmount()));

        assertTrue(clearedCart.getCartItemsList().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(clearedCart.getTotalAmount()));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @WithMockUser(
            username = "rollback.customer@shopsphere.com",
            roles = "CUSTOMER"
    )
    void placeOrder_SecondProductHasInsufficientStock_ShouldRollbackEarlierStockDeduction() {
        User customer = User.builder()
                .userName("Rollback Customer")
                .email("rollback.customer@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("3333333333")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setCategoryName("Rollback Category");
        category.setDescription("Category for transaction rollback test");
        category = categoryRepository.save(category);

        Product availableProduct = new Product();
        availableProduct.setProductName("Available Product");
        availableProduct.setDescription("Product with available stock");
        availableProduct.setPrice(new BigDecimal("100.00"));
        availableProduct.setStockQuantity(10);
        availableProduct.setBrand("ShopSphere");
        availableProduct.setCategory(category);
        availableProduct = productRepository.save(availableProduct);

        Product unavailableProduct = new Product();
        unavailableProduct.setProductName("Unavailable Product");
        unavailableProduct.setDescription("Product without available stock");
        unavailableProduct.setPrice(new BigDecimal("200.00"));
        unavailableProduct.setStockQuantity(0);
        unavailableProduct.setBrand("ShopSphere");
        unavailableProduct.setCategory(category);
        unavailableProduct = productRepository.save(unavailableProduct);

        Cart cart = new Cart();
        cart.setUser(customer);
        cart.setTotalAmount(new BigDecimal("300.00"));
        cart.setCartItemsList(new ArrayList<>());

        CartItems availableItem = new CartItems();
        availableItem.setCart(cart);
        availableItem.setProduct(availableProduct);
        availableItem.setQuantity(1);
        availableItem.setPriceAtAdded(new BigDecimal("100.00"));

        CartItems unavailableItem = new CartItems();
        unavailableItem.setCart(cart);
        unavailableItem.setProduct(unavailableProduct);
        unavailableItem.setQuantity(1);
        unavailableItem.setPriceAtAdded(new BigDecimal("200.00"));

        cart.getCartItemsList().add(availableItem);
        cart.getCartItemsList().add(unavailableItem);
        cartRepository.save(cart);

        assertThrows(IllegalArgumentException.class,
                () -> orderService.placeOrder(null));

        entityManager.clear();

        Product reloadedAvailableProduct = productRepository
                .findById(availableProduct.getProductId())
                .orElseThrow();

        Product reloadedUnavailableProduct = productRepository
                .findById(unavailableProduct.getProductId())
                .orElseThrow();

        Cart unchangedCart = cartRepository
                .findByUser(customer)
                .orElseThrow();

        assertEquals(10, reloadedAvailableProduct.getStockQuantity());
        assertEquals(0, reloadedUnavailableProduct.getStockQuantity());
        assertEquals(2, cartItemsRepository.findByCart(unchangedCart).size());
        assertTrue(orderRepository.findByUser(customer).isEmpty());
    }

    @Test
    void adminCancelsConfirmedOrderWithPartialRefund_ShouldCreateRemainingRefundAndRestoreStock() {
        User customer = User.builder()
                .userName("Admin Cancellation Customer")
                .email("admin.cancellation@shopsphere.com")
                .password("not-used-by-this-test")
                .phoneNo("2222222222")
                .role(Role.CUSTOMER)
                .build();
        customer = userRepository.save(customer);

        Category category = new Category();
        category.setCategoryName("Admin Cancellation Category");
        category.setDescription("Category for admin cancellation test");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setProductName("Admin Cancellation Product");
        product.setDescription("Product for admin cancellation test");
        product.setPrice(new BigDecimal("500.00"));
        product.setStockQuantity(8);
        product.setBrand("ShopSphere");
        product.setCategory(category);
        product = productRepository.save(product);

        Order order = Order.builder()
                .orderNumber("ORD-ADMIN-CANCEL-001")
                .user(customer)
                .orderStatus(OrderStatus.CONFIRMED)
                .totalOrderAmount(new BigDecimal("1000.00"))
                .discountAmount(BigDecimal.ZERO)
                .orderItemsList(new ArrayList<>())
                .build();

        OrderItems orderItem = OrderItems.builder()
                .order(order)
                .product(product)
                .quantity(2)
                .priceAtAdded(new BigDecimal("500.00"))
                .build();

        order.getOrderItemsList().add(orderItem);
        order = orderRepository.save(order);

        Payment payment = Payment.builder()
                .paymentNumber("PAY-ADMIN-CANCEL-001")
                .order(order)
                .user(customer)
                .amount(new BigDecimal("1000.00"))
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentMethod(PaymentMethod.UPI)
                .build();
        payment = paymentRepository.save(payment);

        Refund existingRefund = Refund.builder()
                .payment(payment)
                .refundAmount(new BigDecimal("300.00"))
                .refundReason("Existing partial refund")
                .refundStatus(RefundStatus.COMPLETED)
                .build();
        refundRepository.save(existingRefund);

        orderService.updateOrderStatus(
                order.getOrderId(),
                OrderStatusUpdateRequestDto.builder()
                        .orderStatus(OrderStatus.CANCELLED)
                        .build()
        );

        entityManager.flush();
        entityManager.clear();

        Order cancelledOrder = orderRepository
                .findById(order.getOrderId())
                .orElseThrow();

        Product restoredProduct = productRepository
                .findById(product.getProductId())
                .orElseThrow();

        List<Refund> refunds = refundRepository
                .findByPaymentPaymentId(payment.getPaymentId());

        Refund cancellationRefund = refunds.stream()
                .filter(refund -> refund.getRefundStatus() == RefundStatus.PENDING)
                .findFirst()
                .orElseThrow();

        assertEquals(OrderStatus.CANCELLED, cancelledOrder.getOrderStatus());
        assertEquals(10, restoredProduct.getStockQuantity());
        assertEquals(2, refunds.size());
        assertEquals(0, new BigDecimal("700.00")
                .compareTo(cancellationRefund.getRefundAmount()));
        assertEquals(RefundStatus.PENDING, cancellationRefund.getRefundStatus());
    }
}