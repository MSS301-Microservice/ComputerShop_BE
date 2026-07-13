package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Dùng cho monolith remnant (Order/Payment) gọi sang lấy giỏ hàng thô lúc
 * checkout — chỉ cần id, không cần giá/tên (Order tự lấy giá qua catalog-service). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalCartResponse {
    private int cartId;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private int cartItemId;
        private int variantId;
        private int quantity;
    }
}
