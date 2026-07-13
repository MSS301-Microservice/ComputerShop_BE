package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.dto.response.ReserveStockResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantAttributeResponse;
import sp26.group3.computer.sba301_computershop.dto.response.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.entity.Product;
import sp26.group3.computer.sba301_computershop.entity.ProductItem;
import sp26.group3.computer.sba301_computershop.entity.ProductVariant;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.*;
import sp26.group3.computer.sba301_computershop.service.InternalCatalogService;

import java.util.List;
import java.util.UUID;

/**
 * Các thao tác chỉ dành cho service-to-service call (monolith remnant gọi sang),
 * KHÔNG dành cho Frontend — xem InternalCatalogController.
 */
@Service
@RequiredArgsConstructor
public class InternalCatalogServiceImpl implements InternalCatalogService {

    private final ProductVariantRepository variantRepository;
    private final ProductItemRepository productItemRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantAttributeRepository variantAttributeRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public VariantSummaryResponse getVariantSummary(int variantId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));
        return toSummary(variant);
    }

    @Override
    @Transactional
    public ReserveStockResponse reserveStock(int variantId, int quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));

        if (variant.getStockQuantity() < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        variant.setStockQuantity(variant.getStockQuantity() - quantity);
        variantRepository.save(variant);

        ProductItem item = ProductItem.builder()
                .variant(variant)
                .serialNumber(generateSerialNumber(variant.getSku()))
                .build();
        item = productItemRepository.save(item);

        return ReserveStockResponse.builder()
                .itemId(item.getItemId())
                .serialNumber(item.getSerialNumber())
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .productId(variant.getProduct().getProductId())
                .productName(variant.getProduct().getName())
                .unitPrice(variant.getPrice())
                .warrantyMonths(variant.getProduct().getWarrantyMonths())
                .build();
    }

    @Override
    @Transactional
    public void releaseStock(int variantId, int quantity) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));
        variant.setStockQuantity(variant.getStockQuantity() + quantity);
        variantRepository.save(variant);
    }

    @Override
    @Transactional
    public void releaseStockByItem(int itemId, int quantity) {
        ProductItem item = productItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.VARIANT_NOT_FOUND));
        ProductVariant variant = item.getVariant();
        variant.setStockQuantity(variant.getStockQuantity() + quantity);
        variantRepository.save(variant);
    }

    @Override
    public List<VariantAttributeResponse> getVariantAttributes(int variantId) {
        return variantAttributeRepository.findByVariantVariantId(variantId).stream()
                .map(a -> VariantAttributeResponse.builder()
                        .attributeId(a.getAttribute().getAttributeId())
                        .attributeName(a.getAttribute().getAttributeName())
                        .value(a.getValue())
                        .build())
                .toList();
    }

    @Override
    public List<Integer> getProductIdsByCategory(int categoryId) {
        return productRepository.findByCategoryCategoryId(categoryId).stream()
                .map(Product::getProductId)
                .toList();
    }

    @Override
    public List<Integer> getProductIdsByBrand(int brandId) {
        return productRepository.findByBrandBrandId(brandId).stream()
                .map(Product::getProductId)
                .toList();
    }

    @Override
    public Integer getCategoryIdByName(String categoryName) {
        return categoryRepository.findByCategoryNameIgnoreCase(categoryName)
                .map(c -> c.getCategoryId())
                .orElse(null);
    }

    private VariantSummaryResponse toSummary(ProductVariant variant) {
        Product product = variant.getProduct();
        String thumbnailUrl = productImageRepository
                .findFirstByProductProductIdAndIsThumbnailTrue(product.getProductId())
                .or(() -> productImageRepository.findByProductProductId(product.getProductId()).stream().findFirst())
                .map(img -> img.getImageUrl())
                .orElse(null);

        return VariantSummaryResponse.builder()
                .variantId(variant.getVariantId())
                .productId(product.getProductId())
                .productName(product.getName())
                .sku(variant.getSku())
                .variantName(variant.getVariantName())
                .price(variant.getPrice())
                .stockQuantity(variant.getStockQuantity())
                .thumbnailUrl(thumbnailUrl)
                .build();
    }

    private String generateSerialNumber(String sku) {
        return sku + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
