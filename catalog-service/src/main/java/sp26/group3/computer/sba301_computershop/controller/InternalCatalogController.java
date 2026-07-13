package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.request.ReserveStockRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ReserveStockResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantAttributeResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.service.InternalCatalogService;

import java.util.List;

/**
 * Service-to-service endpoints cho monolith remnant (Cart/Order/PCBuild/Promotion) —
 * KHÔNG dành cho Frontend gọi. Bảo vệ bằng shared secret header thay vì JWT khách hàng.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalCatalogController {

    private final InternalCatalogService internalCatalogService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @GetMapping("/variants/{id}")
    ApiResponse<VariantSummaryResponse> getVariant(
            @PathVariable int id, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<VariantSummaryResponse> res = new ApiResponse<>();
        res.setResult(internalCatalogService.getVariantSummary(id));
        return res;
    }

    @PostMapping("/variants/{id}/reserve-stock")
    ApiResponse<ReserveStockResponse> reserveStock(
            @PathVariable int id, @RequestBody ReserveStockRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<ReserveStockResponse> res = new ApiResponse<>();
        res.setResult(internalCatalogService.reserveStock(id, request.getQuantity()));
        return res;
    }

    @PostMapping("/variants/{id}/release-stock")
    ApiResponse<Void> releaseStock(
            @PathVariable int id, @RequestBody ReserveStockRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        internalCatalogService.releaseStock(id, request.getQuantity());
        return new ApiResponse<>();
    }

    @PostMapping("/items/{itemId}/release-stock")
    ApiResponse<Void> releaseStockByItem(
            @PathVariable int itemId, @RequestBody ReserveStockRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        internalCatalogService.releaseStockByItem(itemId, request.getQuantity());
        return new ApiResponse<>();
    }

    @GetMapping("/variants/{id}/attributes")
    ApiResponse<List<VariantAttributeResponse>> getVariantAttributes(
            @PathVariable int id, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<List<VariantAttributeResponse>> res = new ApiResponse<>();
        res.setResult(internalCatalogService.getVariantAttributes(id));
        return res;
    }

    @GetMapping("/products/ids-by-category/{categoryId}")
    ApiResponse<List<Integer>> getProductIdsByCategory(
            @PathVariable int categoryId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<List<Integer>> res = new ApiResponse<>();
        res.setResult(internalCatalogService.getProductIdsByCategory(categoryId));
        return res;
    }

    @GetMapping("/products/ids-by-brand/{brandId}")
    ApiResponse<List<Integer>> getProductIdsByBrand(
            @PathVariable int brandId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<List<Integer>> res = new ApiResponse<>();
        res.setResult(internalCatalogService.getProductIdsByBrand(brandId));
        return res;
    }

    @GetMapping("/categories/id-by-name/{name}")
    ApiResponse<Integer> getCategoryIdByName(
            @PathVariable String name, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<Integer> res = new ApiResponse<>();
        res.setResult(internalCatalogService.getCategoryIdByName(name));
        return res;
    }
}
