package sp26.group3.computer.sba301_computershop.util;

import jakarta.servlet.http.HttpServletRequest;

/** Trích IP khách hàng từ request gốc tới order-service — dùng khi gọi sang
 * monolith để tạo link VNPay (monolith không còn thấy request gốc của
 * khách vì order-service là nơi nhận request đặt hàng). */
public class IpAddressUtil {

    private IpAddressUtil() {
    }

    public static String getIpAddress(HttpServletRequest request) {
        try {
            String ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            return ipAddress;
        } catch (Exception e) {
            return "Invalid IP:" + e.getMessage();
        }
    }
}
