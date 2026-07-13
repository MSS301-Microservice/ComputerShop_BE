package sp26.group3.computer.sba301_computershop.util;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import sp26.group3.computer.sba301_computershop.exception.AppException;
import sp26.group3.computer.sba301_computershop.exception.ErrorCode;

/**
 * User giờ thuộc user-service (DB riêng) — thay vì tra DB local để lấy user hiện tại
 * (như code cũ làm qua UserRepository.findByEmail), đọc thẳng claim "userId" mà
 * user-service đã nhúng sẵn vào JWT lúc issue token. Tránh phải gọi API/DB cho
 * mỗi request chỉ để biết "user đang đăng nhập là ai".
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    public static int getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            Object userId = jwt.getClaim("userId");
            if (userId != null) {
                return ((Number) userId).intValue();
            }
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    public static String getCurrentUserRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String scope = jwtAuth.getToken().getClaimAsString("scope");
            if (scope != null) {
                return scope;
            }
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    public static String getCurrentUserEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getName();
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
