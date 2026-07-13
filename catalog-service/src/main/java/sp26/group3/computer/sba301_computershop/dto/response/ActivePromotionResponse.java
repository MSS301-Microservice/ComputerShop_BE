package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bản sao shape của ActivePromotionResponse bên monolith remnant. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivePromotionResponse {
    private int discountPercent;
    private String promoCode;
}
