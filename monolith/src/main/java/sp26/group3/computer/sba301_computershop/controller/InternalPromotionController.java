package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.response.ActivePromotionResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.entity.PromotionProduct;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.PromotionProductRepository;

import java.util.List;

/**
 * Service-to-service endpoint cho catalog-service gọi ngược sang lấy khuyến
 * mãi đang active của 1 sản phẩm (Promotion vẫn ở monolith, Product đã tách
 * sang catalog-service — chiều phụ thuộc ngược so với các domain khác).
 */
@RestController
@RequestMapping("/internal/promotions")
@RequiredArgsConstructor
public class InternalPromotionController {

    private final PromotionProductRepository promotionProductRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @GetMapping("/active-for-product/{productId}")
    ApiResponse<ActivePromotionResponse> getActivePromotionForProduct(
            @PathVariable int productId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);

        List<PromotionProduct> promos = promotionProductRepository.findActivePromotionByProductId(productId);
        ApiResponse<ActivePromotionResponse> res = new ApiResponse<>();
        if (!promos.isEmpty()) {
            res.setResult(ActivePromotionResponse.builder()
                    .discountPercent(promos.get(0).getPromotion().getDiscountPercent())
                    .promoCode(promos.get(0).getPromotion().getPromoCode())
                    .build());
        }
        return res;
    }
}
