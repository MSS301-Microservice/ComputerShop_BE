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
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.cart.InternalCartResponse;

/**
 * Gọi trực tiếp sang cart-service (không qua Gateway) để đọc/xóa giỏ hàng lúc
 * checkout — cùng pattern với CatalogServiceClient/UserServiceClient. Dùng
 * shared secret (X-Internal-Api-Key), không phải JWT khách hàng.
 */
@Component
@Slf4j
public class CartServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.cart-service.base-url}")
    private String cartServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private HttpEntity<Void> internalRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return new HttpEntity<>(headers);
    }

    public InternalCartResponse getCartByUser(int userId) {
        try {
            var response = restTemplate.exchange(
                    cartServiceBaseUrl + "/internal/carts/by-user/" + userId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<InternalCartResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (RestClientException e) {
            log.warn("Failed to fetch cart for user {} from cart-service: {}", userId, e.getMessage());
            return null;
        }
    }

    public void deleteCartItem(int cartItemId) {
        try {
            restTemplate.exchange(
                    cartServiceBaseUrl + "/internal/carts/items/" + cartItemId,
                    HttpMethod.DELETE,
                    internalRequestEntity(),
                    Void.class
            );
        } catch (RestClientException e) {
            log.warn("Failed to delete cart item {} on cart-service: {}", cartItemId, e.getMessage());
        }
    }
}
