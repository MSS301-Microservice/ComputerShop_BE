package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.UserResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.service.UserService;

import java.util.List;

/**
 * Service-to-service endpoints dùng cho các microservice khác (vd. monolith remnant)
 * lấy thông tin user để hiển thị (username, roleName...), KHÔNG dành cho Frontend gọi.
 * Bảo vệ bằng shared secret header thay vì JWT khách hàng, vì caller là 1 service khác
 * chứ không phải người dùng đã đăng nhập.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @GetMapping("/{id}")
    ApiResponse<UserResponse> getUserById(
            @PathVariable int id,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUserById(id));
        return apiResponse;
    }

    @GetMapping("/by-role/{roleName}")
    ApiResponse<List<UserResponse>> getUsersByRole(
            @PathVariable String roleName,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<List<UserResponse>> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUsersByRole(roleName));
        return apiResponse;
    }

    @GetMapping("/by-email/{email}")
    ApiResponse<UserResponse> getUserByEmail(
            @PathVariable String email,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.getUserByEmail(email));
        return apiResponse;
    }
}
