package sp26.group3.computer.sba301_computershop.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Dùng khi pcbuilder-service gọi sang monolith để đặt hàng từ 1 PC build —
 * không có JWT khách hàng trong context (gọi bằng X-Internal-Api-Key) nên
 * userId phải truyền tường minh. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalPlaceOrderFromItemsRequest {
    private int userId;
    private List<AddToCartRequest> items;
    private PlaceOrderRequest order;
}
