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

    MAX_UPLOAD_SIZE_EXCEEDED(7003, "File size exceeds the maximum allowed limit (10MB per file)",
            HttpStatus.BAD_REQUEST),

    VARIANT_NOT_FOUND(6101, "Product variant not found", HttpStatus.NOT_FOUND),

    CART_NOT_FOUND(8001, "Cart not found", HttpStatus.NOT_FOUND),
    CART_ITEM_NOT_FOUND(8002, "Cart item not found", HttpStatus.NOT_FOUND),

    ORDER_NOT_FOUND(9001, "Order not found", HttpStatus.NOT_FOUND),
    EMPTY_CART(9002, "Cart is empty, cannot place order", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(9003, "Insufficient stock for variant", HttpStatus.BAD_REQUEST),
    ORDER_ALREADY_PAID(9004, "Order is already fully paid", HttpStatus.BAD_REQUEST),
    INVALID_STATUS_TRANSITION(9005, "Invalid order status transition", HttpStatus.BAD_REQUEST),
    INSTALLMENT_PACKAGE_NOT_FOUND(9006, "Installment package not found", HttpStatus.NOT_FOUND),
    INSTALLMENT_MIN_AMOUNT_NOT_MET(9007, "Order total amount does not meet package minimum order amount",
            HttpStatus.BAD_REQUEST),
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
