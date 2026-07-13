package sp26.group3.computer.sba301_computershop.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO trả về cho order-service khi cần validate gói trả góp lúc đặt hàng. */
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
