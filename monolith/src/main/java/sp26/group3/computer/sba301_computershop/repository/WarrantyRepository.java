package sp26.group3.computer.sba301_computershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sp26.group3.computer.sba301_computershop.entity.Warranty;

import java.util.List;
import java.util.Optional;

public interface WarrantyRepository extends JpaRepository<Warranty, Integer> {
    List<Warranty> findByOrderId(int orderId);

    Optional<Warranty> findByOrderItemId(int orderItemId);

    // Đổi từ tra cứu theo Order.user.phoneNumber (User giờ ở user-service, DB khác,
    // không JOIN được nữa) sang recipientPhone lưu trực tiếp trên Warranty —
    // đúng với nghiệp vụ tra cứu bảo hành theo SĐT nhận hàng lúc đặt, không cần đăng nhập.
    List<Warranty> findByRecipientPhone(String phoneNumber);
}
