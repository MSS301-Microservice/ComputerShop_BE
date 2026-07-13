package sp26.group3.computer.sba301_computershop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Bản rút gọn của UserResponse bên user-service, dùng khi monolith remnant
 * cần hiển thị thông tin người dùng (username, roleName...) trong response
 * của chính nó (đơn hàng, blog, chat...) mà không sở hữu bảng users nữa.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {
    private int userId;
    private String username;
    private String email;
    private String roleName;
}
