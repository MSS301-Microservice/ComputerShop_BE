package sp26.group3.computer.sba301_computershop.dto.request.warranty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** order-service gọi sang monolith khi đơn hàng chuyển sang DELIVERED để
 * monolith tạo bảo hành (Warranty vẫn ở monolith, chưa tách) — truyền kèm
 * snapshot cần thiết vì monolith không còn JPA relationship tới OrderItem. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarrantiesRequest {
    private int orderId;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private int orderItemId;
        private String serialNumber;
        private int productId;
        private String productName;
        private int warrantyMonths;
        private String recipientPhone;
    }
}
