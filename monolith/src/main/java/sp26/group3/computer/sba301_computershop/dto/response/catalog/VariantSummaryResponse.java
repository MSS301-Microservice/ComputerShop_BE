package sp26.group3.computer.sba301_computershop.dto.response.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bản sao shape của VariantSummaryResponse bên catalog-service — dùng khi
 * monolith remnant cần đọc giá/tồn kho/tên sản phẩm để hiển thị (Cart/PCBuild). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantSummaryResponse {
    private int variantId;
    private int productId;
    private String productName;
    private String sku;
    private String variantName;
    private double price;
    private int stockQuantity;
    private String thumbnailUrl;
}
