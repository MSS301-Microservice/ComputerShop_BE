package sp26.group3.computer.sba301_computershop.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;

import java.util.List;

/** Bản rút gọn của OrderResponse — dùng khi monolith remnant (Payment/
 * Warranty/Reporting) cần đọc dữ liệu đơn hàng cơ bản mà không cần kéo theo
 * payment schedule (tránh gọi vòng lại monolith không cần thiết). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalOrderResponse {
    private int orderId;
    private int userId;
    private double totalAmount;
    private PaymentMode paymentMode;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private int orderItemId;
        private int variantId;
        private int quantity;
    }
}
