package sp26.group3.computer.sba301_computershop.service;

import sp26.group3.computer.sba301_computershop.dto.response.ReserveStockResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantAttributeResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantSummaryResponse;

import java.util.List;

public interface InternalCatalogService {

    VariantSummaryResponse getVariantSummary(int variantId);

    ReserveStockResponse reserveStock(int variantId, int quantity);

    void releaseStock(int variantId, int quantity);

    void releaseStockByItem(int itemId, int quantity);

    List<VariantAttributeResponse> getVariantAttributes(int variantId);

    List<Integer> getProductIdsByCategory(int categoryId);

    List<Integer> getProductIdsByBrand(int brandId);

    Integer getCategoryIdByName(String categoryName);
}
