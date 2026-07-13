package sp26.group3.computer.sba301_computershop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_KEY(1005, "Invalid key", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY(1008, "Invalid JSON request body", HttpStatus.BAD_REQUEST),

    // File errors (kept because GlobalExceptionHandler references it generically)
    MAX_UPLOAD_SIZE_EXCEEDED(7003, "File size exceeds the maximum allowed limit (10MB per file)",
            HttpStatus.BAD_REQUEST),

    // Catalog (dùng khi gọi catalog-service không tìm thấy variant/hết hàng —
    // CatalogServiceClient dùng chung code với các service khác nên giữ cả 2)
    VARIANT_NOT_FOUND(6101, "Product variant not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK(9003, "Insufficient stock for variant", HttpStatus.BAD_REQUEST),

    // Cart errors
    CART_NOT_FOUND(8001, "Cart not found", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(8002, "Cart item not found", HttpStatus.NOT_FOUND),
    CART_ITEM_ALREADY_EXISTS(8003, "Variant already exists in cart", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(8004, "Quantity must be greater than 0", HttpStatus.BAD_REQUEST),
    PRODUCT_OUT_OF_STOCK(8005, "Product is out of stock", HttpStatus.BAD_REQUEST),
    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
