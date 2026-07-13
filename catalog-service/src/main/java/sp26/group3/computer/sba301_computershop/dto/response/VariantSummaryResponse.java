package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dùng cho các service khác (Cart/Order/PCBuild) gọi sang catalog-service để
 * hiển thị/tính toán — thay cho việc JOIN trực tiếp bảng product_variants
 * như trước khi tách service.
 */
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
