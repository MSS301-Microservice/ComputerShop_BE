package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.response.ActivePromotionResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;

/**
 * Gọi ngược sang monolith remnant để lấy khuyến mãi đang active cho 1 sản
 * phẩm — Promotion vẫn ở monolith, Product đã tách sang đây. Chiều phụ thuộc
 * ngược so với các client khác (UserServiceClient/CatalogServiceClient ở
 * monolith gọi TỚI các service; ở đây catalog-service gọi NGƯỢC lại monolith).
 */
@Component
@Slf4j
public class PromotionServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.monolith.base-url}")
    private String monolithBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public ActivePromotionResponse getActivePromotion(int productId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            var response = restTemplate.exchange(
                    monolithBaseUrl + "/internal/promotions/active-for-product/" + productId,
                    HttpMethod.GET,
                    new HttpEntity<Void>(headers),
                    new ParameterizedTypeReference<ApiResponse<ActivePromotionResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (RestClientException e) {
            log.warn("Failed to fetch active promotion for product {}: {}", productId, e.getMessage());
            return null;
        }
    }
}
