package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.request.payment.CreatePaymentSchedulesRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.VnpayUrlRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentScheduleResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.InstallmentPackageResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.VnpayUrlResponse;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.service.PaymentService;

import java.util.List;

/**
 * Service-to-service endpoints cho order-service gọi sang — Payment/
 * Installment (InstallmentPackage, OrderPaymentSchedule) vẫn ở monolith,
 * chưa tách. KHÔNG dành cho Frontend, bảo vệ bằng X-Internal-Api-Key.
 */
@RestController
@RequestMapping("/internal/payment")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @GetMapping("/installment-packages/{id}")
    ApiResponse<InstallmentPackageResponse> getInstallmentPackage(
            @PathVariable int id, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<InstallmentPackageResponse> res = new ApiResponse<>();
        res.setResult(paymentService.getInstallmentPackage(id));
        return res;
    }

    @PostMapping("/schedules")
    ApiResponse<Void> createPaymentSchedules(
            @RequestBody CreatePaymentSchedulesRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        paymentService.createPaymentSchedulesForOrder(request);
        return new ApiResponse<>();
    }

    @GetMapping("/schedules/by-order/{orderId}")
    ApiResponse<List<PaymentScheduleResponse>> getPaymentSchedules(
            @PathVariable int orderId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        ApiResponse<List<PaymentScheduleResponse>> res = new ApiResponse<>();
        res.setResult(paymentService.getPaymentSchedulesByOrder(orderId));
        return res;
    }

    @PostMapping("/vnpay-url")
    ApiResponse<VnpayUrlResponse> createVnPayUrl(
            @RequestBody VnpayUrlRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        String url = paymentService.createVnPayUrlInternal(request);
        ApiResponse<VnpayUrlResponse> res = new ApiResponse<>();
        res.setResult(VnpayUrlResponse.builder().paymentUrl(url).build());
        return res;
    }
}
