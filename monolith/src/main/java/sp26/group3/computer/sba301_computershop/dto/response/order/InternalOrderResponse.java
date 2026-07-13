package sp26.group3.computer.sba301_computershop.dto.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;

import java.util.List;

/** Bản rút gọn của OrderResponse bên order-service — dùng khi monolith
 * remnant (Payment/Warranty/Reporting) cần đọc dữ liệu đơn hàng mà không
 * còn sở hữu bảng orders/order_items nữa. */
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
