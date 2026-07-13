package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.request.AddToCartRequest;
import sp26.group3.computer.sba301_computershop.dto.request.PlaceOrderRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.OrderResponse;

import java.util.List;
import java.util.Map;

/**
 * Gọi sang order-service để đặt hàng từ 1 PC build (build → order) — Order
 * đã tách khỏi monolith (Giai đoạn 5), PCBuild đã tách trước đó (Giai đoạn 4).
 * Dùng X-Internal-Api-Key vì đây là service-to-service call, không phải
 * request trực tiếp từ khách hàng.
 */
@Component
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.order-service.base-url}")
    private String orderServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public OrderResponse placeOrderFromItems(int userId, List<AddToCartRequest> items, PlaceOrderRequest orderRequest) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "userId", userId,
                "items", items,
                "order", orderRequest
        );

        var response = restTemplate.exchange(
                orderServiceBaseUrl + "/internal/orders/from-items",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                new ParameterizedTypeReference<ApiResponse<OrderResponse>>() {}
        );
        return response.getBody() != null ? response.getBody().getResult() : null;
    }
}
