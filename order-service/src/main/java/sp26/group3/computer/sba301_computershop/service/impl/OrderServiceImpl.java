package sp26.group3.computer.sba301_computershop.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.client.CartServiceClient;
import sp26.group3.computer.sba301_computershop.client.CatalogServiceClient;
import sp26.group3.computer.sba301_computershop.client.PaymentServiceClient;
import sp26.group3.computer.sba301_computershop.client.PromotionServiceClient;
import sp26.group3.computer.sba301_computershop.client.UserServiceClient;
import sp26.group3.computer.sba301_computershop.client.WarrantyServiceClient;
import sp26.group3.computer.sba301_computershop.dto.request.AddToCartRequest;
import sp26.group3.computer.sba301_computershop.dto.request.PlaceOrderRequest;
import sp26.group3.computer.sba301_computershop.dto.request.UpdateOrderStatusRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.CreatePaymentSchedulesRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.VnpayUrlRequest;
import sp26.group3.computer.sba301_computershop.dto.request.warranty.CreateWarrantiesRequest;
import sp26.group3.computer.sba301_computershop.dto.response.*;
import sp26.group3.computer.sba301_computershop.dto.response.cart.InternalCartResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.ReserveStockResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.InstallmentPackageResponse;
import sp26.group3.computer.sba301_computershop.entity.Order;
import sp26.group3.computer.sba301_computershop.entity.OrderItem;
import sp26.group3.computer.sba301_computershop.enums.OrderStatus;
import sp26.group3.computer.sba301_computershop.enums.PaymentMethod;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.OrderItemRepository;
import sp26.group3.computer.sba301_computershop.repository.OrderRepository;
import sp26.group3.computer.sba301_computershop.service.OrderService;
import sp26.group3.computer.sba301_computershop.util.IpAddressUtil;
import sp26.group3.computer.sba301_computershop.util.SecurityUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    OrderItemRepository orderItemRepository;
    CartServiceClient cartServiceClient;
    UserServiceClient userServiceClient;
    CatalogServiceClient catalogServiceClient;
    PromotionServiceClient promotionServiceClient;
    PaymentServiceClient paymentServiceClient;
    WarrantyServiceClient warrantyServiceClient;

    // ======================== PLACE ORDER ========================

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, HttpServletRequest servletRequest) {
        int userId = SecurityUtils.getCurrentUserId();
        InternalCartResponse cart = cartServiceClient.getCartByUser(userId);
        if (cart == null || cart.getCartId() == 0) {
            throw new AppException(ErrorCode.CART_NOT_FOUND);
        }

        List<InternalCartResponse.Item> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new AppException(ErrorCode.EMPTY_CART);
        }

        // Filter specifically chosen items if present in request
        if (request.getVariantIds() != null && !request.getVariantIds().isEmpty()) {
            cartItems = cartItems.stream()
                    .filter(item -> request.getVariantIds().contains(item.getVariantId()))
                    .collect(java.util.stream.Collectors.toList());
            if (cartItems.isEmpty()) {
                throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
            }
        }

        // 1. Load variant info (giá, tồn kho) từ catalog-service, validate stock
        Map<Integer, VariantSummaryResponse> variantMap = new HashMap<>();
        for (InternalCartResponse.Item cartItem : cartItems) {
            VariantSummaryResponse variant = catalogServiceClient.getVariant(cartItem.getVariantId());
            if (variant == null) {
                throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
            }
            if (variant.getStockQuantity() < cartItem.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            variantMap.put(cartItem.getVariantId(), variant);
        }

        // 2. Calculate total (apply active promotions — Promotion vẫn ở monolith)
        double totalAmount = cartItems.stream()
                .mapToDouble(item -> getEffectivePrice(variantMap.get(item.getVariantId())) * item.getQuantity())
                .sum();

        // 3. Validate installment package (nếu có) — InstallmentPackage vẫn ở monolith
        Integer packageId = null;
        if (request.getPaymentMode() == PaymentMode.INSTALLMENT) {
            packageId = validateInstallmentPackage(request.getPackageId(), totalAmount);
        }

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .paymentMode(request.getPaymentMode())
                .installmentPackageId(packageId)
                .build();
        order = orderRepository.save(order);
        log.info("Created order | orderId={} totalAmount={}", order.getOrderId(), totalAmount);

        // 4. Reserve stock ở catalog-service (trừ tồn kho + tạo ProductItem/serial
        // trong transaction cục bộ của catalog-service) rồi tạo OrderItem cục bộ
        List<OrderItem> orderItems = new ArrayList<>();
        for (InternalCartResponse.Item cartItem : cartItems) {
            VariantSummaryResponse variant = variantMap.get(cartItem.getVariantId());
            ReserveStockResponse reserved = catalogServiceClient.reserveStock(cartItem.getVariantId(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemId(reserved.getItemId())
                    .variantId(variant.getVariantId())
                    .sku(reserved.getSku())
                    .variantName(reserved.getVariantName())
                    .productId(reserved.getProductId())
                    .productName(reserved.getProductName())
                    .serialNumber(reserved.getSerialNumber())
                    .warrantyMonths(reserved.getWarrantyMonths())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(getEffectivePrice(variant))
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .shippingAddress(request.getShippingAddress())
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }

        // 5. Create payment schedule (Payment vẫn ở monolith)
        paymentServiceClient.createPaymentSchedules(CreatePaymentSchedulesRequest.builder()
                .orderId(order.getOrderId())
                .userId(userId)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .paymentMode(request.getPaymentMode())
                .installmentPackageId(packageId)
                .build());

        // 6. Clear selected cart items (Only clear now for COD. For others, clear on payment success)
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            for (InternalCartResponse.Item item : cartItems) {
                cartServiceClient.deleteCartItem(item.getCartItemId());
            }
            log.info("Cleared selected cart items after placing COD order | cartId={}", cart.getCartId());
        }

        // 7. Handle Payment URL for VNPAY
        String paymentUrl = null;
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentUrl = paymentServiceClient.createVnPayUrl(VnpayUrlRequest.builder()
                    .orderId(order.getOrderId())
                    .ipAddress(IpAddressUtil.getIpAddress(servletRequest))
                    .build());
        }

        return toOrderResponse(order, orderItems, paymentUrl);
    }

    // ======================== PLACE ORDER FROM BUILD ITEMS ========================

    @Override
    @Transactional
    public OrderResponse placeOrderFromItems(List<AddToCartRequest> items,
            PlaceOrderRequest request,
            HttpServletRequest servletRequest) {
        return placeOrderFromItemsForUser(SecurityUtils.getCurrentUserId(), items, request, servletRequest);
    }

    @Override
    @Transactional
    public OrderResponse placeOrderFromItemsForUser(int userId, List<AddToCartRequest> items,
            PlaceOrderRequest request,
            HttpServletRequest servletRequest) {
        // 1. Load variants and validate stock
        Map<Integer, VariantSummaryResponse> variantMap = new HashMap<>();
        for (AddToCartRequest item : items) {
            VariantSummaryResponse variant = catalogServiceClient.getVariant(item.getVariantId());
            if (variant == null) {
                throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
            }
            if (variant.getStockQuantity() < item.getQuantity()) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
            variantMap.put(item.getVariantId(), variant);
        }

        // 2. Calculate total (apply active promotions)
        double totalAmount = items.stream()
                .mapToDouble(i -> getEffectivePrice(variantMap.get(i.getVariantId())) * i.getQuantity())
                .sum();

        // 3. Validate installment package (nếu có)
        Integer packageId = null;
        if (request.getPaymentMode() == PaymentMode.INSTALLMENT) {
            packageId = validateInstallmentPackage(request.getPackageId(), totalAmount);
        }

        Order order = Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .orderDate(LocalDateTime.now())
                .paymentMethod(request.getPaymentMethod())
                .paymentMode(request.getPaymentMode())
                .installmentPackageId(packageId)
                .build();
        order = orderRepository.save(order);
        log.info("Created order from build | orderId={} totalAmount={}", order.getOrderId(), totalAmount);

        // 4. Reserve stock + tạo OrderItem (no cart clearing)
        List<OrderItem> orderItems = new ArrayList<>();
        for (AddToCartRequest item : items) {
            VariantSummaryResponse variant = variantMap.get(item.getVariantId());
            ReserveStockResponse reserved = catalogServiceClient.reserveStock(item.getVariantId(), item.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .itemId(reserved.getItemId())
                    .variantId(variant.getVariantId())
                    .sku(reserved.getSku())
                    .variantName(reserved.getVariantName())
                    .productId(reserved.getProductId())
                    .productName(reserved.getProductName())
                    .serialNumber(reserved.getSerialNumber())
                    .warrantyMonths(reserved.getWarrantyMonths())
                    .quantity(item.getQuantity())
                    .unitPrice(getEffectivePrice(variant))
                    .recipientName(request.getRecipientName())
                    .recipientPhone(request.getRecipientPhone())
                    .shippingAddress(request.getShippingAddress())
                    .build();
            orderItems.add(orderItemRepository.save(orderItem));
        }

        // 5. Payment schedule
        paymentServiceClient.createPaymentSchedules(CreatePaymentSchedulesRequest.builder()
                .orderId(order.getOrderId())
                .userId(userId)
                .totalAmount(totalAmount)
                .paymentMethod(request.getPaymentMethod())
                .paymentMode(request.getPaymentMode())
                .installmentPackageId(packageId)
                .build());

        // 6. VNPay URL
        String paymentUrl = null;
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            paymentUrl = paymentServiceClient.createVnPayUrl(VnpayUrlRequest.builder()
                    .orderId(order.getOrderId())
                    .ipAddress(IpAddressUtil.getIpAddress(servletRequest))
                    .build());
        }

        return toOrderResponse(order, orderItems, paymentUrl);
    }

    // ======================== GET ORDER BY ID ========================

    @Override
    public OrderResponse getOrderById(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        return toOrderResponse(order, items);
    }

    // ======================== GET MY ORDERS ========================

    @Override
    public List<OrderResponse> getMyOrders() {
        int userId = SecurityUtils.getCurrentUserId();
        List<Order> orders = orderRepository.findByUserIdOrderByOrderDateDesc(userId);

        return orders.stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
    }

    // ======================== GET ALL ORDERS (ADMIN/STAFF) ========================

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAllByOrderByOrderDateDesc().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
    }

    // ======================== PAGED: GET MY ORDERS ========================

    @Override
    public PagedResponse<OrderResponse> getMyOrdersPaged(Pageable pageable) {
        int userId = SecurityUtils.getCurrentUserId();
        Page<Order> page = orderRepository.findByUserId(userId, pageable);
        List<OrderResponse> content = page.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ======================== PAGED: GET ALL ORDERS (ADMIN/STAFF) ========================

    @Override
    public PagedResponse<OrderResponse> getAllOrdersPaged(Pageable pageable) {
        Page<Order> page = orderRepository.findAll(pageable);
        List<OrderResponse> content = page.getContent().stream()
                .map(order -> {
                    List<OrderItem> items = orderItemRepository.findByOrderOrderId(order.getOrderId());
                    return toOrderResponse(order, items);
                })
                .toList();
        return PagedResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    // ======================== UPDATE ORDER STATUS ========================

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(int orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getStatus().canTransitionTo(request.getStatus())) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        order.setStatus(request.getStatus());
        orderRepository.save(order);
        log.info("Updated order status | orderId={} status={}", orderId, request.getStatus());

        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);

        if (request.getStatus() == OrderStatus.DELIVERED) {
            List<CreateWarrantiesRequest.Item> warrantyItems = items.stream()
                    .map(item -> CreateWarrantiesRequest.Item.builder()
                            .orderItemId(item.getOrderItemId())
                            .serialNumber(item.getSerialNumber())
                            .productId(item.getProductId())
                            .productName(item.getProductName())
                            .warrantyMonths(item.getWarrantyMonths())
                            .recipientPhone(item.getRecipientPhone())
                            .build())
                    .toList();
            warrantyServiceClient.createWarrantiesForOrder(CreateWarrantiesRequest.builder()
                    .orderId(orderId)
                    .items(warrantyItems)
                    .build());
        }

        return toOrderResponse(order, items);
    }

    // ======================== CANCEL ORDER ========================

    @Override
    @Transactional
    public void cancelOrder(int orderId) {
        int userId = SecurityUtils.getCurrentUserId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // Owners or Shop (Staff/Admin) can cancel
        boolean isOwner = order.getUserId() == userId;
        String roleName = SecurityUtils.getCurrentUserRole();
        boolean isShop = roleName.equals("ADMIN") || roleName.equals("STAFF");

        if (!isOwner && !isShop) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!order.getStatus().canTransitionTo(OrderStatus.CANCELLED)) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }

        // Restore stock ở catalog-service — cần variantId, nhưng OrderItem chỉ
        // lưu snapshot sku/tên, không lưu variantId. Trả lại tồn kho theo item
        // (catalog-service tự biết variant nào tương ứng qua itemId).
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);
        for (OrderItem item : items) {
            catalogServiceClient.releaseStockByItem(item.getItemId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        log.info("Cancelled order | orderId={}", orderId);
    }

    @Override
    public OrderPaymentResultResponse getOrderPaymentResult(int orderId, Integer installmentNo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        List<PaymentScheduleResponse> schedules = paymentServiceClient.getPaymentSchedules(orderId);

        boolean isPaid;
        double amount;
        if (installmentNo != null && installmentNo > 0) {
            PaymentScheduleResponse specific = schedules.stream()
                    .filter(s -> s.getInstallmentNo() == installmentNo)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Installment not found"));
            isPaid = (specific.getStatus() == sp26.group3.computer.sba301_computershop.enums.PaymentStatus.PAID);
            amount = specific.getAmount() + specific.getPenaltyAmount();
        } else {
            isPaid = schedules.stream()
                    .anyMatch(s -> s.getStatus() == sp26.group3.computer.sba301_computershop.enums.PaymentStatus.PAID);
            amount = order.getTotalAmount();
        }

        return OrderPaymentResultResponse.builder()
                .orderId(order.getOrderId())
                .amount(amount)
                .status(isPaid ? "success" : "failed")
                .installmentNo((installmentNo == null || installmentNo == 0) ? null : installmentNo)
                .build();
    }

    // ======================== HELPER METHODS ========================

    private Integer validateInstallmentPackage(Integer packageId, double totalAmount) {
        if (packageId == null) {
            throw new AppException(ErrorCode.INSTALLMENT_PACKAGE_NOT_FOUND);
        }
        InstallmentPackageResponse pack = paymentServiceClient.getInstallmentPackage(packageId);
        if (pack == null) {
            throw new AppException(ErrorCode.INSTALLMENT_PACKAGE_NOT_FOUND);
        }
        if (totalAmount < pack.getMinOrderAmount()) {
            throw new AppException(ErrorCode.INSTALLMENT_MIN_AMOUNT_NOT_MET);
        }
        return pack.getPackageId();
    }

    private double getEffectivePrice(VariantSummaryResponse variant) {
        ActivePromotionResponse promo = promotionServiceClient.getActivePromotion(variant.getProductId());
        if (promo != null && promo.getDiscountPercent() > 0) {
            return variant.getPrice() * (1 - promo.getDiscountPercent() / 100.0);
        }
        return variant.getPrice();
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items, String paymentUrl) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::toOrderItemResponse)
                .toList();

        List<PaymentScheduleResponse> paymentResponses = paymentServiceClient.getPaymentSchedules(order.getOrderId());

        UserSummaryResponse orderUser = userServiceClient.getUser(order.getUserId());
        return OrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .username(orderUser != null ? orderUser.getUsername() : null)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentMode(order.getPaymentMode())
                .orderDate(order.getOrderDate())
                .items(itemResponses)
                .payments(paymentResponses)
                .paymentUrl(paymentUrl)
                .build();
    }

    private OrderResponse toOrderResponse(Order order, List<OrderItem> items) {
        return toOrderResponse(order, items, null);
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        // Dùng snapshot lưu tại thời điểm đặt hàng (không gọi lại catalog-service) —
        // sản phẩm/biến thể có thể đã bị xóa/sửa sau đó, lịch sử đơn hàng vẫn hiển
        // thị đúng dữ liệu tại thời điểm mua.
        return OrderItemResponse.builder()
                .orderItemId(item.getOrderItemId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(item.getUnitPrice() * item.getQuantity())
                .variantId(item.getVariantId())
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .recipientName(item.getRecipientName())
                .recipientPhone(item.getRecipientPhone())
                .shippingAddress(item.getShippingAddress())
                .serialNumber(item.getSerialNumber())
                .build();
    }
}
