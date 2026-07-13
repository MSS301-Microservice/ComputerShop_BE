package sp26.group3.computer.sba301_computershop.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sp26.group3.computer.sba301_computershop.enums.PaymentMethod;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;

/** order-service gọi sang monolith ngay sau khi tạo Order để monolith tạo
 * các dòng order_payment_schedule tương ứng (Payment/Installment vẫn ở
 * monolith, chưa tách). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentSchedulesRequest {
    private int orderId;
    private int userId;
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentMode paymentMode;
    private Integer installmentPackageId;
}
