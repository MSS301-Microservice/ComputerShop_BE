package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dùng khi catalog-service gọi sang monolith để lấy khuyến mãi đang active
 * cho 1 sản phẩm (tính giá hiển thị trên trang chi tiết sản phẩm). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivePromotionResponse {
    private int discountPercent;
    private String promoCode;
}
