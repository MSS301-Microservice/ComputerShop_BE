package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import sp26.group3.computer.sba301_computershop.client.CatalogServiceClient;
import sp26.group3.computer.sba301_computershop.client.OrderServiceClient;
import sp26.group3.computer.sba301_computershop.client.PromotionServiceClient;
import sp26.group3.computer.sba301_computershop.dto.request.AddBuildItemRequest;
import sp26.group3.computer.sba301_computershop.dto.request.AddToCartRequest;
import sp26.group3.computer.sba301_computershop.dto.request.CompatibleVariantsRequest;
import sp26.group3.computer.sba301_computershop.dto.request.PlaceOrderRequest;
import sp26.group3.computer.sba301_computershop.dto.request.SaveBuildNameRequest;
import sp26.group3.computer.sba301_computershop.dto.response.*;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantAttributeResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.entity.*;
import sp26.group3.computer.sba301_computershop.enums.BuildStatus;
import sp26.group3.computer.sba301_computershop.enums.ComponentType;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.*;
import sp26.group3.computer.sba301_computershop.service.CompatibilityService;
import sp26.group3.computer.sba301_computershop.service.PCBuildService;
import sp26.group3.computer.sba301_computershop.util.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PCBuildServiceImpl implements PCBuildService {

    PCBuildRepository pcBuildRepository;
    PCBuildItemRepository pcBuildItemRepository;
    CatalogServiceClient catalogServiceClient;
    CompatibilityService compatibilityService;
    OrderServiceClient orderServiceClient;
    PromotionServiceClient promotionServiceClient;

    // ======================== UPSERT ITEM ========================

    @Override
    @Transactional
    public PCBuildResponse upsertItem(AddBuildItemRequest request) {
        int userId = SecurityUtils.getCurrentUserId();
        PCBuild build = pcBuildRepository
                .findByUserIdAndStatus(userId, BuildStatus.DRAFT)
                .orElseGet(() -> createNewDraft(userId));
        VariantSummaryResponse variant = catalogServiceClient.getVariant(request.getVariantId());
        if (variant == null) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
        }

        // RAM slot check: kiểm tra tổng quantity sau khi upsert không vượt quá slot mainboard
        if (request.getComponentType() == ComponentType.RAM) {
            validateRamSlots(build, request.getVariantId(), request.getQuantity());
        }

        double price = effectivePrice(variant);
        boolean isMultiSlot = request.getComponentType().isMultiSlot();
        if (isMultiSlot) {
            pcBuildItemRepository
                    .findByBuildBuildIdAndComponentTypeAndVariantId(
                            build.getBuildId(), request.getComponentType(), request.getVariantId())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setQuantity(request.getQuantity());
                                existing.setPrice(price);
                                pcBuildItemRepository.save(existing);
                                log.info("[PCBuild] Updated multi-slot item | buildId={} componentType={} variantId={}",
                                        build.getBuildId(), request.getComponentType(), variant.getVariantId());
                            },
                            () -> {
                                pcBuildItemRepository.save(PCBuildItem.builder()
                                        .build(build)
                                        .componentType(request.getComponentType())
                                        .variantId(variant.getVariantId())
                                        .quantity(request.getQuantity())
                                        .price(price)
                                        .build());
                                log.info("[PCBuild] Added multi-slot item | buildId={} componentType={} variantId={}",
                                        build.getBuildId(), request.getComponentType(), variant.getVariantId());
                            }
                    );
        } else {
            // Single-slot: overwrite nếu type đã có, insert nếu chưa
            pcBuildItemRepository
                    .findByBuildBuildIdAndComponentType(build.getBuildId(), request.getComponentType())
                    .ifPresentOrElse(
                            existing -> {
                                existing.setVariantId(variant.getVariantId());
                                existing.setPrice(price);
                                existing.setQuantity(request.getQuantity());
                                pcBuildItemRepository.save(existing);
                                log.info("[PCBuild] Overwrite item | buildId={} componentType={} variantId={}",
                                        build.getBuildId(), request.getComponentType(), variant.getVariantId());
                            },
                            () -> {
                                pcBuildItemRepository.save(PCBuildItem.builder()
                                        .build(build)
                                        .componentType(request.getComponentType())
                                        .variantId(variant.getVariantId())
                                        .quantity(request.getQuantity())
                                        .price(price)
                                        .build());
                                log.info("[PCBuild] Added item | buildId={} componentType={} variantId={}",
                                        build.getBuildId(), request.getComponentType(), variant.getVariantId());
                            }
                    );
        }

        return reloadAndUpdate(build);
    }

    // ======================== SAVE DRAFT ========================

    @Override
    @Transactional
    public PCBuildResponse saveBuild(SaveBuildNameRequest request) {
        int userId = SecurityUtils.getCurrentUserId();
        PCBuild build = pcBuildRepository
                .findByUserIdAndStatus(userId, BuildStatus.DRAFT)
                .orElseThrow(() -> new AppException(ErrorCode.PC_BUILD_NOT_FOUND));

        build.setBuildName(request.getBuildName());
        build.setStatus(BuildStatus.SAVED);
        build.setUpdatedAt(LocalDateTime.now());
        pcBuildRepository.save(build);

        log.info("[PCBuild] Saved build | buildId={} name='{}'", build.getBuildId(), request.getBuildName());
        return toBuildResponse(build);
    }

    // ======================== ORDER FROM BUILD ========================

    @Override
    @Transactional
    public OrderResponse orderFromBuild(PlaceOrderRequest request, HttpServletRequest httpRequest) {
        int userId = SecurityUtils.getCurrentUserId();
        // Hỗ trợ order từ cả DRAFT lẫn SAVED
        PCBuild build = pcBuildRepository
                .findByUserIdAndStatus(userId, BuildStatus.DRAFT)
                .or(() -> pcBuildRepository
                        .findByUserIdAndStatusOrderByCreatedAtDesc(userId, BuildStatus.SAVED)
                        .stream().findFirst())
                .orElseThrow(() -> new AppException(ErrorCode.PC_BUILD_NOT_FOUND));

        List<PCBuildItem> items = build.getItems();
        if (items == null || items.isEmpty()) {
            throw new AppException(ErrorCode.PC_BUILD_INCOMPLETE);
        }

        List<AddToCartRequest> orderItems = items.stream()
                .map(i -> AddToCartRequest.builder()
                        .variantId(i.getVariantId())
                        .quantity(i.getQuantity())
                        .build())
                .toList();

        OrderResponse orderResponse = orderServiceClient.placeOrderFromItems(userId, orderItems, request);

        build.setStatus(BuildStatus.ORDERED);
        build.setUpdatedAt(LocalDateTime.now());
        pcBuildRepository.save(build);

        log.info("[PCBuild] Ordered from build | buildId={} orderId={}", build.getBuildId(), orderResponse.getOrderId());
        return orderResponse;
    }

    // ======================== COMPATIBLE VARIANTS ========================

    @Override
    public CompatibleVariantsResponse getCompatibleVariants(
            List<CompatibleVariantsRequest.ItemHint> items, ComponentType targetType) {
        return compatibilityService.getFilterHints(items, targetType);
    }

    // ======================== REMOVE ITEM ========================

    @Override
    @Transactional
    public PCBuildResponse removeItem(int buildItemId) {
        int userId = SecurityUtils.getCurrentUserId();
        PCBuildItem item = pcBuildItemRepository.findById(buildItemId)
                .orElseThrow(() -> new AppException(ErrorCode.PC_BUILD_NOT_FOUND));

        PCBuild build = item.getBuild();
        if (build.getStatus() != BuildStatus.DRAFT
                || build.getUserId() != userId) {
            throw new AppException(ErrorCode.PC_BUILD_NOT_FOUND);
        }

        pcBuildItemRepository.delete(item);
        log.info("[PCBuild] Removed item | buildItemId={} buildId={}", buildItemId, build.getBuildId());
        return reloadAndUpdate(build);
    }

    // ======================== PRIVATE HELPERS ========================

    /** Trả về giá sau khuyến mãi nếu có, ngược lại trả về giá gốc (dữ liệu Promotion vẫn ở monolith) */
    private double effectivePrice(VariantSummaryResponse variant) {
        var activePromo = promotionServiceClient.getActivePromotion(variant.getProductId());
        return activePromo != null
                ? variant.getPrice() * (1 - activePromo.getDiscountPercent() / 100.0)
                : variant.getPrice();
    }

    private void validateRamSlots(PCBuild build, int upsertVariantId, int newQuantity) {
        Optional<PCBuildItem> mainboardOpt = pcBuildItemRepository
                .findByBuildBuildIdAndComponentType(build.getBuildId(), ComponentType.MAINBOARD);
        if (mainboardOpt.isEmpty()) return; // chưa có mainboard → chưa biết giới hạn

        String ramSlotsStr = catalogServiceClient.getVariantAttributes(mainboardOpt.get().getVariantId())
                .stream()
                .filter(a -> a.getAttributeName().equalsIgnoreCase("RAM Slots"))
                .map(VariantAttributeResponse::getValue)
                .findFirst()
                .orElse(null);
        if (ramSlotsStr == null) return;

        int maxSlots;
        try {
            maxSlots = Integer.parseInt(ramSlotsStr.trim());
        } catch (NumberFormatException e) {
            return;
        }

        // Tính tổng quantity của tất cả RAM hiện tại, trừ variant đang upsert (sẽ bị overwrite)
        int usedSlots = pcBuildItemRepository
                .findAllByBuildBuildIdAndComponentType(build.getBuildId(), ComponentType.RAM)
                .stream()
                .filter(i -> i.getVariantId() != upsertVariantId)
                .mapToInt(PCBuildItem::getQuantity)
                .sum();

        if (usedSlots + newQuantity > maxSlots) {
            log.warn("[PCBuild] RAM slots exceeded: buildId={} maxSlots={} usedSlots={} newQty={}",
                    build.getBuildId(), maxSlots, usedSlots, newQuantity);
            throw new AppException(ErrorCode.RAM_SLOTS_EXCEEDED);
        }
    }

    private PCBuild createNewDraft(int userId) {
        LocalDateTime now = LocalDateTime.now();
        PCBuild draft = PCBuild.builder()
                .userId(userId)
                .status(BuildStatus.DRAFT)
                .totalPrice(0.0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        log.info("[PCBuild] Created new DRAFT build for userId={}", userId);
        return pcBuildRepository.save(draft);
    }

    // ======================== GET DRAFT ========================

    @Override
    @Transactional(readOnly = true)
    public PCBuildResponse getDraft() {
        int userId = SecurityUtils.getCurrentUserId();
        PCBuild build = pcBuildRepository
                .findByUserIdAndStatus(userId, BuildStatus.DRAFT)
                .orElseGet(() -> createNewDraft(userId));
        return toBuildResponse(build);
    }

    // ======================== GET MY BUILDS ========================

    @Override
    @Transactional(readOnly = true)
    public List<PCBuildResponse> getMyBuilds() {
        int userId = SecurityUtils.getCurrentUserId();
        return pcBuildRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toBuildResponse)
                .toList();
    }

    private PCBuildResponse reloadAndUpdate(PCBuild build) {
        // Use JOIN FETCH query to bypass Hibernate L1 cache and get fresh items from DB
        PCBuild fresh = pcBuildRepository.findWithItemsById(build.getBuildId()).orElseThrow();
        double total = fresh.getItems().stream()
                .mapToDouble(i -> {
                    VariantSummaryResponse v = catalogServiceClient.getVariant(i.getVariantId());
                    return v != null ? effectivePrice(v) * i.getQuantity() : 0;
                })
                .sum();
        fresh.setTotalPrice(total);
        fresh.setUpdatedAt(LocalDateTime.now());
        pcBuildRepository.save(fresh);
        return toBuildResponse(fresh);
    }

    // ======================== MAPPERS ========================

    private PCBuildResponse toBuildResponse(PCBuild build) {
        List<PCBuildItemResponse> itemResponses = build.getItems() == null ? List.of()
                : build.getItems().stream().map(this::toItemResponse).filter(java.util.Objects::nonNull).toList();

        return PCBuildResponse.builder()
                .buildId(build.getBuildId())
                .buildName(build.getBuildName())
                .status(build.getStatus())
                .totalPrice(build.getTotalPrice())
                .createdAt(build.getCreatedAt())
                .updatedAt(build.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    private PCBuildItemResponse toItemResponse(PCBuildItem item) {
        VariantSummaryResponse variant = catalogServiceClient.getVariant(item.getVariantId());
        if (variant == null) {
            return null;
        }

        // Tính discount giống CartService (dữ liệu Promotion vẫn ở monolith)
        Double discountedPrice = null;
        double discountPercent = 0.0;
        var activePromo = promotionServiceClient.getActivePromotion(variant.getProductId());
        if (activePromo != null) {
            discountPercent = activePromo.getDiscountPercent();
            discountedPrice = variant.getPrice() * (1 - discountPercent / 100.0);
        }

        double effectivePrice = discountedPrice != null ? discountedPrice : variant.getPrice();

        return PCBuildItemResponse.builder()
                .buildItemId(item.getBuildItemId())
                .componentType(item.getComponentType().name())
                .componentTypeName(item.getComponentType().getDisplayName())
                .variantId(variant.getVariantId())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .discountedPrice(discountedPrice)
                .discountPercent(discountPercent)
                .quantity(item.getQuantity())
                .subtotal(effectivePrice * item.getQuantity())
                .productId(variant.getProductId())
                .productName(variant.getProductName())
                .thumbnailUrl(variant.getThumbnailUrl())
                .build();
    }
}
