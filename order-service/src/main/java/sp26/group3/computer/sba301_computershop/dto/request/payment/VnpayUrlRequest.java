package sp26.group3.computer.sba301_computershop.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** order-service gọi sang monolith để lấy link thanh toán VNPay cho 1 đơn
 * hàng — IP khách hàng được order-service tự trích xuất từ request gốc
 * (không dùng lại HttpServletRequest của lời gọi nội bộ). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnpayUrlRequest {
    private int orderId;
    private String bankCode;
    private Integer installmentNo;
    private String ipAddress;
}
