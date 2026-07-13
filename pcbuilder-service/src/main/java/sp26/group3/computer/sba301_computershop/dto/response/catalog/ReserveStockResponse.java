package sp26.group3.computer.sba301_computershop.dto.response.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Bản sao shape của ReserveStockResponse bên catalog-service. */
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
