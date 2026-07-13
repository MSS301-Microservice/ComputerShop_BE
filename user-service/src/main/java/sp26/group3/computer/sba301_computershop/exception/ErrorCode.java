package sp26.group3.computer.sba301_computershop.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED(1001, "User already exists", HttpStatus.BAD_REQUEST),
    ROLE_NOT_FOUND(1002, "Role not found", HttpStatus.NOT_FOUND),
    ROLE_EXISTED(1012, "Role already exists", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1004, "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_KEY(1005, "Invalid key", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY(1008, "Invalid JSON request body", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1010, "User not existed", HttpStatus.NOT_FOUND),
    // Create user errors
    EMAIL_INVALID(1006, "EMAIL_INVALID", HttpStatus.BAD_REQUEST),
    EMAIL_REQUIRED(1009, "EMAIL_REQUIRED", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1007, "PASSWORD_INVALID", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1016, "PASSWORD_REQUIRED", HttpStatus.BAD_REQUEST),

    // File errors (kept because GlobalExceptionHandler references it generically)
    MAX_UPLOAD_SIZE_EXCEEDED(7003, "File size exceeds the maximum allowed limit (10MB per file)",
            HttpStatus.BAD_REQUEST),

    // reset password errors
    PASSWORD_MISMATCH(1014, "The verification password does not match", HttpStatus.BAD_REQUEST),
    PASSWORD_SAME_AS_OLD(1015, "The new password must not be the same as the old password", HttpStatus.BAD_REQUEST),
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
