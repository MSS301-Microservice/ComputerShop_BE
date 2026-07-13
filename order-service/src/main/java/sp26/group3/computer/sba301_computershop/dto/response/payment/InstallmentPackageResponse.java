package sp26.group3.computer.sba301_computershop.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bản sao shape của InstallmentPackage bên monolith — InstallmentPackage vẫn
 * ở monolith (Payment/Installment chưa tách), order-service chỉ đọc qua
 * PaymentServiceClient để validate lúc đặt hàng trả góp. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstallmentPackageResponse {
    private int packageId;
    private String name;
    private int durationMonths;
    private double interestRate;
    private double minOrderAmount;
    private double downPaymentPercentage;
    private boolean active;
}
