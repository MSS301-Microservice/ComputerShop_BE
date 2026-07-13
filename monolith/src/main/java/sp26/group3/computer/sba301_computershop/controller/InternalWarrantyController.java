package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.request.warranty.CreateWarrantiesRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.service.WarrantyService;

/**
 * Service-to-service endpoint cho order-service gọi sang khi đơn hàng
 * chuyển sang DELIVERED để tạo bảo hành — Warranty vẫn ở monolith, chưa
 * tách. KHÔNG dành cho Frontend, bảo vệ bằng X-Internal-Api-Key.
 */
@RestController
@RequestMapping("/internal/warranties")
@RequiredArgsConstructor
public class InternalWarrantyController {

    private final WarrantyService warrantyService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @PostMapping("/create-for-order")
    ApiResponse<Void> createWarrantiesForOrder(
            @RequestBody CreateWarrantiesRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        warrantyService.createWarrantiesForOrder(request);
        return new ApiResponse<>();
    }
}
