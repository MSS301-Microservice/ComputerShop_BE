package sp26.group3.computer.sba301_computershop.service;

import jakarta.servlet.http.HttpServletRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.CreatePaymentSchedulesRequest;
import sp26.group3.computer.sba301_computershop.dto.request.payment.VnpayUrlRequest;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentDTO;
import sp26.group3.computer.sba301_computershop.dto.response.PaymentScheduleResponse;
import sp26.group3.computer.sba301_computershop.dto.response.payment.InstallmentPackageResponse;

import java.util.List;
import java.util.Map;

public interface PaymentService {
    PaymentDTO createVnPayPayment(HttpServletRequest request, int orderId, String bankCode, Integer installmentNo);

    String handleVnPayCallback(HttpServletRequest request);

    //void handleVnPayIpn(HttpServletRequest request);
    Map<String, String> handleVnPayIpn(HttpServletRequest request);

    // ==== Dùng bởi order-service qua InternalPaymentController (Giai đoạn 5) ====

    InstallmentPackageResponse getInstallmentPackage(int packageId);

    void createPaymentSchedulesForOrder(CreatePaymentSchedulesRequest request);

    List<PaymentScheduleResponse> getPaymentSchedulesByOrder(int orderId);

    String createVnPayUrlInternal(VnpayUrlRequest request);
}
