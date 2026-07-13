package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.request.warranty.CreateWarrantiesRequest;

/**
 * Gọi ngược sang monolith remnant để tạo bảo hành khi đơn hàng chuyển sang
 * DELIVERED — Warranty vẫn ở monolith, chưa tách.
 */
@Component
@Slf4j
public class WarrantyServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.monolith.base-url}")
    private String monolithBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public void createWarrantiesForOrder(CreateWarrantiesRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Api-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.exchange(
                    monolithBaseUrl + "/internal/warranties/create-for-order",
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to create warranties for orderId={}: {}", request.getOrderId(), e.getMessage());
        }
    }
}
