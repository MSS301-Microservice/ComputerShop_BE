package sp26.group3.computer.sba301_computershop.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.UserSummaryResponse;

import java.util.List;

/**
 * Gọi trực tiếp sang user-service (không qua Gateway) để lấy thông tin user
 * cần hiển thị (username, roleName...). Đây là giao tiếp REST đồng bộ giữa
 * 2 service nội bộ, dùng shared secret (X-Internal-Api-Key) thay vì JWT khách hàng
 * vì bên gọi là 1 service, không phải người dùng đã đăng nhập.
 */
@Component
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${services.user-service.base-url}")
    private String userServiceBaseUrl;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private HttpEntity<Void> internalRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Api-Key", internalApiKey);
        return new HttpEntity<>(headers);
    }

    public UserSummaryResponse getUser(int userId) {
        try {
            var response = restTemplate.exchange(
                    userServiceBaseUrl + "/internal/users/" + userId,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserSummaryResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch user {} from user-service: {}", userId, e.getMessage());
            return null;
        }
    }

    public UserSummaryResponse getUserByEmail(String email) {
        try {
            var response = restTemplate.exchange(
                    userServiceBaseUrl + "/internal/users/by-email/" + email,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new org.springframework.core.ParameterizedTypeReference<ApiResponse<UserSummaryResponse>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch user by email {} from user-service: {}", email, e.getMessage());
            return null;
        }
    }

    public List<UserSummaryResponse> getUsersByRole(String roleName) {
        try {
            var response = restTemplate.exchange(
                    userServiceBaseUrl + "/internal/users/by-role/" + roleName,
                    HttpMethod.GET,
                    internalRequestEntity(),
                    new org.springframework.core.ParameterizedTypeReference<ApiResponse<List<UserSummaryResponse>>>() {}
            );
            return response.getBody() != null ? response.getBody().getResult() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch users by role {} from user-service: {}", roleName, e.getMessage());
            return List.of();
        }
    }
}
