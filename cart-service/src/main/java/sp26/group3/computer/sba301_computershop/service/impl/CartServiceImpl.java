package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.client.CatalogServiceClient;
import sp26.group3.computer.sba301_computershop.client.PromotionServiceClient;
import sp26.group3.computer.sba301_computershop.dto.request.AddToCartRequest;
import sp26.group3.computer.sba301_computershop.dto.request.UpdateCartItemRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ActivePromotionResponse;
import sp26.group3.computer.sba301_computershop.dto.response.CartItemResponse;
import sp26.group3.computer.sba301_computershop.dto.response.CartResponse;
import sp26.group3.computer.sba301_computershop.dto.response.catalog.VariantSummaryResponse;
import sp26.group3.computer.sba301_computershop.entity.*;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.*;
import sp26.group3.computer.sba301_computershop.service.CartService;
import sp26.group3.computer.sba301_computershop.util.SecurityUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CartServiceImpl implements CartService {

    CartRepository cartRepository;
    CartItemRepository cartItemRepository;
    CatalogServiceClient catalogServiceClient;
    PromotionServiceClient promotionServiceClient;

    // ======================== GET MY CART ========================

    @Override
    public CartResponse getMyCart() {
        int userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        return toCartResponse(cart);
    }

    // ======================== ADD TO CART ========================

    @Override
    @Transactional
    public CartResponse addToCart(AddToCartRequest request) {
        int userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        VariantSummaryResponse variant = catalogServiceClient.getVariant(request.getVariantId());
        if (variant == null) {
            throw new AppException(ErrorCode.VARIANT_NOT_FOUND);
        }

        int stock = variant.getStockQuantity();

        Optional<CartItem> existingItem = cartItemRepository
                .findByCartCartIdAndVariantId(cart.getCartId(), variant.getVariantId());

        if (existingItem.isPresent()) {

            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + request.getQuantity();

            if (newQuantity > stock) {
                throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            item.setQuantity(newQuantity);
            cartItemRepository.save(item);

            log.info("Merged cart item | cartItemId={} newQty={}", item.getCartItemId(), item.getQuantity());

        } else {

            if (request.getQuantity() > stock) {
                throw new AppException(ErrorCode.PRODUCT_OUT_OF_STOCK);
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .variantId(variant.getVariantId())
                    .quantity(request.getQuantity())
                    .build();

            cartItemRepository.save(newItem);

            log.info("Added new cart item | variantId={} qty={}", variant.getVariantId(), request.getQuantity());
        }

        cart = cartRepository.findById(cart.getCartId()).orElseThrow();
        return toCartResponse(cart);
    }
    // ======================== UPDATE CART ITEM ========================

    @Override
    @Transactional
    public CartResponse updateCartItem(int cartItemId, UpdateCartItemRequest request) {
        int userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        // Ensure item belongs to current user's cart
        if (item.getCart().getCartId() != cart.getCartId()) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        if (request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
        log.info("Updated cart item | cartItemId={} newQty={}", cartItemId, request.getQuantity());

        cart = cartRepository.findById(cart.getCartId()).orElseThrow();
        return toCartResponse(cart);
    }

    // ======================== REMOVE CART ITEM ========================

    @Override
    @Transactional
    public CartResponse removeCartItem(int cartItemId) {
        int userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (item.getCart().getCartId() != cart.getCartId()) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        cart.getCartItems().remove(item);
        cartRepository.save(cart);
        log.info("Removed cart item | cartItemId={}", cartItemId);

        cart = cartRepository.findById(cart.getCartId()).orElseThrow();
        return toCartResponse(cart);
    }

    // ======================== CLEAR CART ========================

    @Override
    @Transactional
    public CartResponse clearCart() {
        int userId = SecurityUtils.getCurrentUserId();
        Cart cart = getOrCreateCart(userId);

        cartItemRepository.deleteAllByCartCartId(cart.getCartId());
        log.info("Cleared cart | cartId={}", cart.getCartId());

        cart = cartRepository.findById(cart.getCartId()).orElseThrow();
        cart.getCartItems().clear();
        return toCartResponse(cart);
    }

    // ======================== HELPER METHODS ========================

    private Cart getOrCreateCart(int userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .cartItems(new ArrayList<>())
                            .build();
                    log.info("Created new cart for userId={}", userId);
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse toCartResponse(Cart cart) {
        List<CartItem> items = cart.getCartItems() != null ? cart.getCartItems() : List.of();

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toCartItemResponse)
                .filter(java.util.Objects::nonNull)
                .toList();

        double totalPrice = itemResponses.stream()
                .mapToDouble(i -> {
                    double effectivePrice = i.getDiscountedPrice() != null ? i.getDiscountedPrice() : i.getPrice();
                    return effectivePrice * i.getQuantity();
                })
                .sum();

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .totalItems(totalItems)
                .build();
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        VariantSummaryResponse variant = catalogServiceClient.getVariant(item.getVariantId());
        if (variant == null) {
            // Variant đã bị xóa bên catalog-service — bỏ qua dòng này khi hiển thị giỏ hàng.
            return null;
        }

        // Apply active promotion if any (dữ liệu Promotion vẫn ở monolith)
        Double discountedPrice = null;
        double discountPercent = 0.0;
        ActivePromotionResponse activePromo = promotionServiceClient.getActivePromotion(variant.getProductId());
        if (activePromo != null) {
            discountPercent = activePromo.getDiscountPercent();
            discountedPrice = variant.getPrice() * (1 - discountPercent / 100.0);
        }

        return CartItemResponse.builder()
                .cartItemId(item.getCartItemId())
                .variantId(variant.getVariantId())
                .variantName(variant.getVariantName())
                .sku(variant.getSku())
                .price(variant.getPrice())
                .discountedPrice(discountedPrice)
                .discountPercent(discountPercent)
                .stockQuantity(variant.getStockQuantity())
                .quantity(item.getQuantity())
                .productId(variant.getProductId())
                .productName(variant.getProductName())
                .thumbnailUrl(variant.getThumbnailUrl())
                .build();
    }
}
