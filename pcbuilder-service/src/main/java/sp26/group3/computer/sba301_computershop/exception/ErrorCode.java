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
    INSUFFICIENT_STOCK(9003, "Insufficient stock for variant", HttpStatus.BAD_REQUEST),

    ORDER_NOT_FOUND(9001, "Order not found", HttpStatus.NOT_FOUND),

    // PC Build errors
    PC_BUILD_NOT_FOUND(10001, "PC build not found", HttpStatus.NOT_FOUND),
    PC_BUILD_ACCESS_DENIED(10002, "You do not have access to this PC build", HttpStatus.FORBIDDEN),
    PC_BUILD_INCOMPLETE(10003, "PC build has no items", HttpStatus.BAD_REQUEST),
    PC_BUILD_INVALID(10004, "PC build has compatibility issues", HttpStatus.BAD_REQUEST),
    RAM_SLOTS_EXCEEDED(10005, "Mainboard RAM slots are full", HttpStatus.BAD_REQUEST),
    PC_BUILD_ITEM_NOT_FOUND(10006, "PC build item not found", HttpStatus.NOT_FOUND),
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
