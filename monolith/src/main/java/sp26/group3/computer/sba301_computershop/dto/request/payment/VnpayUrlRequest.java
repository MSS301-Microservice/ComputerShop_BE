package sp26.group3.computer.sba301_computershop.dto.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
