package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import sp26.group3.computer.sba301_computershop.enums.OrderStatus;
import sp26.group3.computer.sba301_computershop.enums.PaymentMethod;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private int orderId;

    // User giờ thuộc user-service (DB riêng) — chỉ giữ id thô, không còn JPA relationship.
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private PaymentMode paymentMode;

    // InstallmentPackage vẫn ở monolith (Payment/Installment chưa tách) —
    // chỉ giữ id thô, validate/tính toán lịch trả góp qua PaymentServiceClient.
    @Column(name = "installment_package_id")
    private Integer installmentPackageId;

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    // order_payment_schedule vẫn ở monolith — không còn JPA relationship,
    // đọc qua PaymentServiceClient khi cần hiển thị (toOrderResponse).
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}
