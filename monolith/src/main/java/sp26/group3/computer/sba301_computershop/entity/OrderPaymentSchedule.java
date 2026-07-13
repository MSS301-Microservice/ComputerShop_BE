package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sp26.group3.computer.sba301_computershop.enums.PaymentMode;
import sp26.group3.computer.sba301_computershop.enums.PaymentStatus;

import java.time.LocalDate;

@Entity
@Table(name = "order_payment_schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_schedule_id")
    private int paymentScheduleId;

    // Order giờ thuộc order-service (DB riêng) — không còn JPA relationship.
    // userId/totalAmount/paymentMode được denormalize ngay lúc tạo lịch thanh
    // toán để job nền + báo cáo trả góp không cần gọi REST lặp lại.
    @Column(name = "order_id", nullable = false)
    private int orderId;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "total_amount")
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode")
    private PaymentMode paymentMode;

    // InstallmentPackage vẫn local ở monolith — giữ FK JPA bình thường.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installment_package_id")
    private InstallmentPackage installmentPackage;

    @Column(name = "installment_no", nullable = false)
    private int installmentNo; // Kỳ thứ mấy (1, 2, 3...)

    @Column(name = "amount", nullable = false)
    private double amount; // Số tiền phải đóng của riêng kỳ này

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate; // Hạn chót phải đóng tiền

    @Column(name = "paid_date")
    private LocalDate paidDate; // Ngày khách thực tế bấm thanh toán qua VNPay

    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo; // Lưu mã giao dịch VNPay trả về cho TỪNG KỲ để đối soát

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status; // UNPAID, PAID, OVERDUE

    @Column(name = "penalty_amount")
    @Builder.Default
    private double penaltyAmount = 0; // The total accumulated penalty fee
}
