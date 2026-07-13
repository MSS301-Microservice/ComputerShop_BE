package sp26.group3.computer.sba301_computershop.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.request.InternalPlaceOrderFromItemsRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.OrderResponse;
import sp26.group3.computer.sba301_computershop.dto.response.order.InternalOrderResponse;
import sp26.group3.computer.sba301_computershop.dto.response.report.RevenuePeriodDto;
import sp26.group3.computer.sba301_computershop.dto.response.report.TopProductDto;
import sp26.group3.computer.sba301_computershop.entity.Order;
import sp26.group3.computer.sba301_computershop.entity.OrderItem;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.OrderItemRepository;
import sp26.group3.computer.sba301_computershop.repository.OrderRepository;
import sp26.group3.computer.sba301_computershop.service.OrderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service-to-service endpoint cho pcbuilder-service gọi sang để đặt hàng từ
 * 1 PC build (build → order), và cho monolith remnant (Payment/Warranty/
 * Reporting) đọc dữ liệu đơn hàng cơ bản. KHÔNG dành cho Frontend, bảo vệ
 * bằng X-Internal-Api-Key thay vì JWT khách hàng.
 */
@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @PostMapping("/from-items")
    ApiResponse<OrderResponse> placeOrderFromItems(
            @RequestBody InternalPlaceOrderFromItemsRequest body,
            HttpServletRequest servletRequest,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);

        ApiResponse<OrderResponse> res = new ApiResponse<>();
        res.setResult(orderService.placeOrderFromItemsForUser(
                body.getUserId(), body.getItems(), body.getOrder(), servletRequest));
        return res;
    }

    @GetMapping("/{orderId}")
    ApiResponse<InternalOrderResponse> getOrder(
            @PathVariable int orderId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        List<OrderItem> items = orderItemRepository.findByOrderOrderId(orderId);

        ApiResponse<InternalOrderResponse> res = new ApiResponse<>();
        res.setResult(InternalOrderResponse.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount() != null ? order.getTotalAmount() : 0)
                .paymentMode(order.getPaymentMode())
                .items(items.stream()
                        .map(i -> InternalOrderResponse.Item.builder()
                                .orderItemId(i.getOrderItemId())
                                .variantId(i.getVariantId())
                                .quantity(i.getQuantity())
                                .build())
                        .toList())
                .build());
        return res;
    }

    @GetMapping("/revenue")
    ApiResponse<List<RevenuePeriodDto>> getRevenue(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam String groupBy,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);

        var projections = switch (groupBy) {
            case "MONTH" -> orderRepository.findRevenueGroupByMonth(from, to);
            case "YEAR" -> orderRepository.findRevenueGroupByYear(from, to);
            default -> orderRepository.findRevenueGroupByDay(from, to);
        };

        ApiResponse<List<RevenuePeriodDto>> res = new ApiResponse<>();
        res.setResult(projections.stream()
                .map(p -> RevenuePeriodDto.builder()
                        .period(p.getPeriod())
                        .revenue(p.getRevenue() != null ? p.getRevenue() : 0)
                        .orderCount(p.getOrderCount() != null ? p.getOrderCount() : 0)
                        .build())
                .toList());
        return res;
    }

    @GetMapping("/top-products")
    ApiResponse<List<TopProductDto>> getTopProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(23, 59, 59);

        ApiResponse<List<TopProductDto>> res = new ApiResponse<>();
        res.setResult(orderItemRepository.findTopProducts(from, to).stream()
                .map(p -> TopProductDto.builder()
                        .productName(p.getProductName())
                        .variantName(p.getVariantName())
                        .totalSold(p.getTotalSold() != null ? p.getTotalSold() : 0)
                        .revenue(p.getRevenue() != null ? p.getRevenue() : 0)
                        .build())
                .toList());
        return res;
    }
}
