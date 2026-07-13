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
import sp26.group3.computer.sba301_computershop.dto.response.order.InternalOrderResponse;
import sp26.group3.computer.sba301_computershop.dto.response.report.RevenuePeriodDto;
import sp26.group3.computer.sba301_computershop.dto.response.report.TopProductDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Gọi trực tiếp sang order-service (không qua Gateway) — Order/OrderItem đã
 * tách khỏi monolith. Payment/Warranty/Reporting (còn ở monolith) dùng client
 * này thay cho OrderRepository/OrderItemRepository trực tiếp trước đây.
 */
@Component
@Slf4j
public class OrderServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.order-service.base-url}")
    private String orderServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private HttpEntity<Void> internalRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return new HttpEntity<>(headers);
    }

    public InternalOrderResponse getOrder(int orderId) {
        try {
            var response = restTemplate.exchange(
                    orderServiceBaseUrl + "/internal/orders/" + orderId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<InternalOrderResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (RestClientException e) {
            log.warn("Failed to fetch order {} from order-service: {}", orderId, e.getMessage());
            return null;
        }
    }

    public List<RevenuePeriodDto> getRevenue(LocalDate fromDate, LocalDate toDate, String groupBy) {
        try {
            String url = orderServiceBaseUrl + "/internal/orders/revenue?fromDate=" + fromDate
                    + "&toDate=" + toDate + "&groupBy=" + groupBy;
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<RevenuePeriodDto>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch revenue from order-service: {}", e.getMessage());
            return List.of();
        }
    }

    public List<TopProductDto> getTopProducts(LocalDate fromDate, LocalDate toDate) {
        try {
            String url = orderServiceBaseUrl + "/internal/orders/top-products?fromDate=" + fromDate
                    + "&toDate=" + toDate;
            var response = restTemplate.exchange(
                    url, HttpMethod.GET, internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<TopProductDto>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch top products from order-service: {}", e.getMessage());
            return List.of();
        }
    }
}
