package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.ReserveStockResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantAttributeResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;

import java.util.List;

/**
 * Gọi trực tiếp sang catalog-service (không qua Gateway) để đọc/ghi dữ liệu
 * Product/ProductVariant — cùng pattern với UserServiceClient. Dùng shared
 * secret (X-Internal-Api-Key), không phải JWT khách hàng.
 */
@Component
@Slf4j
public class CatalogServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.catalog-service.base-url}")
    private String catalogServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private HttpEntity<Void> internalRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return new HttpEntity<>(headers);
    }

    private <T> HttpEntity<T> internalRequestEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    public VariantSummaryResponse getVariant(int variantId) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/variants/" + variantId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<VariantSummaryResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
        } catch (RestClientException e) {
            log.warn("Failed to fetch variant {} from catalog-service: {}", variantId, e.getMessage());
            return null;
        }
    }

    /** Trừ tồn kho + tạo ProductItem (serial) trong 1 transaction của catalog-service. */
    public ReserveStockResponse reserveStock(int variantId, int quantity) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/variants/" + variantId + "/reserve-stock",
                    HttpMethod.POST,
                    internalRequestEntity(new QuantityBody(quantity)),
                    new ParameterizedTypeReference<ApiResponse<ReserveStockResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (HttpClientErrorException.NotFound e) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
        } catch (HttpClientErrorException.BadRequest e) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
    }

    /** Hoàn lại tồn kho khi hủy đơn. */
    public void releaseStock(int variantId, int quantity) {
        try {
            restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/variants/" + variantId + "/release-stock",
                    HttpMethod.POST,
                    internalRequestEntity(new QuantityBody(quantity)),
                    Void.class
            );
        } catch (RestClientException e) {
            log.warn("Failed to release stock for variant {}: {}", variantId, e.getMessage());
        }
    }

    /** Hoàn lại tồn kho khi hủy đơn — dùng itemId (OrderItem chỉ lưu snapshot,
     * không lưu variantId), catalog-service tự tra ra variant tương ứng. */
    public void releaseStockByItem(int itemId, int quantity) {
        try {
            restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/items/" + itemId + "/release-stock",
                    HttpMethod.POST,
                    internalRequestEntity(new QuantityBody(quantity)),
                    Void.class
            );
        } catch (RestClientException e) {
            log.warn("Failed to release stock for item {}: {}", itemId, e.getMessage());
        }
    }

    public List<VariantAttributeResponse> getVariantAttributes(int variantId) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/variants/" + variantId + "/attributes",
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<VariantAttributeResponse>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch attributes for variant {}: {}", variantId, e.getMessage());
            return List.of();
        }
    }

    public List<Integer> getProductIdsByCategory(int categoryId) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/products/ids-by-category/" + categoryId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<Integer>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch product ids for category {}: {}", categoryId, e.getMessage());
            return List.of();
        }
    }

    public List<Integer> getProductIdsByBrand(int brandId) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/products/ids-by-brand/" + brandId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<Integer>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch product ids for brand {}: {}", brandId, e.getMessage());
            return List.of();
        }
    }

    public Integer getCategoryIdByName(String categoryName) {
        try {
            var response = restTemplate.exchange(
                    catalogServiceBaseUrl + "/internal/categories/id-by-name/" + categoryName,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<Integer>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (RestClientException e) {
            log.warn("Failed to fetch category id for name {}: {}", categoryName, e.getMessage());
            return null;
        }
    }

    private record QuantityBody(int quantity) {}
}
