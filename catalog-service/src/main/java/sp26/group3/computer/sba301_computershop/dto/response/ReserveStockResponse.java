package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trả về sau khi order-side (monolith remnant) gọi reserve-stock: catalog-service
 * đã tự trừ tồn kho VÀ tạo sẵn ProductItem (serial) trong 1 transaction cục bộ
 * của chính nó — order-side chỉ cần lưu lại itemId làm reference thô.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReserveStockResponse {
    private int itemId;
    private String serialNumber;
    private String sku;
    private String variantName;
    private int productId;
    private String productName;
    private double unitPrice;
    private int warrantyMonths;
}
