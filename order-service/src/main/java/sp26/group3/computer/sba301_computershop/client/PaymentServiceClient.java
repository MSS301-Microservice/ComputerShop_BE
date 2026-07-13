package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.request.payment.CreatePaymentSchedulesRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.VnpayUrlRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentScheduleResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.InstallmentPackageResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.VnpayUrlResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;

import java.util.List;

/**
 * Gọi ngược sang monolith remnant — Payment/Installment (InstallmentPackage,
 * OrderPaymentSchedule) vẫn ở monolith, chưa tách. order-service chỉ sở hữu
 * orders/order_items, mọi thao tác liên quan lịch thanh toán/trả góp/VNPay
 * đều phải đi qua REST thay vì JOIN trực tiếp như trước.
 */
@Component
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.monolith.base-url}")
    private String monolithBaseUrl;

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
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    public InstallmentPackageResponse getInstallmentPackage(int packageId) {
        try {
            var response = restTemplate.exchange(
                    monolithBaseUrl + "/internal/payment/installment-packages/" + packageId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<InstallmentPackageResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (RestClientException e) {
            log.warn("Failed to fetch installment package {}: {}", packageId, e.getMessage());
            return null;
        }
    }

    public void createPaymentSchedules(CreatePaymentSchedulesRequest request) {
        try {
            restTemplate.exchange(
                    monolithBaseUrl + "/internal/payment/schedules",
                    HttpMethod.POST,
                    internalRequestEntity(request),
                    Void.class
            );
        } catch (RestClientException e) {
            log.error("Failed to create payment schedules for orderId={}: {}", request.getOrderId(), e.getMessage());
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    public List<PaymentScheduleResponse> getPaymentSchedules(int orderId) {
        try {
            var response = restTemplate.exchange(
                    monolithBaseUrl + "/internal/payment/schedules/by-order/" + orderId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new ParameterizedTypeReference<ApiResponse<List<PaymentScheduleResponse>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (RestClientException e) {
            log.warn("Failed to fetch payment schedules for orderId={}: {}", orderId, e.getMessage());
            return List.of();
        }
    }

    public String createVnPayUrl(VnpayUrlRequest request) {
        try {
            var response = restTemplate.exchange(
                    monolithBaseUrl + "/internal/payment/vnpay-url",
                    HttpMethod.POST,
                    internalRequestEntity(request),
                    new ParameterizedTypeReference<ApiResponse<VnpayUrlResponse>>() {}
            );
            return response.getBody() != null && response.getBody().getResult() != null
                    ? response.getBody().getResult().getPaymentUrl() : null;
        } catch (RestClientException e) {
            log.error("Failed to create VNPay url for orderId={}: {}", request.getOrderId(), e.getMessage());
            return null;
        }
    }
}
