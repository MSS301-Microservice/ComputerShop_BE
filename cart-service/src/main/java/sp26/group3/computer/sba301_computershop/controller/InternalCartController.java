package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.InternalCartResponse;
import sp26.group3.computer.sba301_computershop.entity.Cart;
import sp26.group3.computer.sba301_computershop.entity.CartItem;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;
import sp26.group3.computer.sba301_computershop.repository.CartItemRepository;
import sp26.group3.computer.sba301_computershop.repository.CartRepository;

import java.util.List;

/**
 * Service-to-service endpoints cho monolith remnant (Order/Payment) gọi sang
 * lúc checkout — đọc giỏ hàng thô và xóa item sau khi đặt hàng thành công.
 * KHÔNG dành cho Frontend, bảo vệ bằng X-Internal-Api-Key.
 */
@RestController
@RequestMapping("/internal/carts")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    private void verifyInternalKey(String providedKey) {
        if (providedKey == null || !providedKey.equals(internalApiKey)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    @GetMapping("/by-user/{userId}")
    ApiResponse<InternalCartResponse> getCartByUser(
            @PathVariable int userId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);

        ApiResponse<InternalCartResponse> res = new ApiResponse<>();
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            res.setResult(InternalCartResponse.builder().cartId(0).items(List.of()).build());
            return res;
        }

        List<CartItem> items = cart.getCartItems() != null ? cart.getCartItems() : List.of();
        res.setResult(InternalCartResponse.builder()
                .cartId(cart.getCartId())
                .items(items.stream()
                        .map(i -> InternalCartResponse.Item.builder()
                                .cartItemId(i.getCartItemId())
                                .variantId(i.getVariantId())
                                .quantity(i.getQuantity())
                                .build())
                        .toList())
                .build());
        return res;
    }

    @DeleteMapping("/items/{cartItemId}")
    ApiResponse<Void> deleteCartItem(
            @PathVariable int cartItemId, @RequestHeader("X-Internal-Api-Key") String apiKey) {
        verifyInternalKey(apiKey);
        cartItemRepository.deleteById(cartItemId);
        return new ApiResponse<>();
    }
}
