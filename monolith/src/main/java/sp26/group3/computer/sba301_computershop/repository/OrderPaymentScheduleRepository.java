package sp26.group3.computer.sba301_computershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sp26.group3.computer.sba301_computershop.entity.OrderPaymentSchedule;
import sp26.group3.computer.sba301_computershop.repository.projection.InstallmentOrderProjection;
import sp26.group3.computer.sba301_computershop.repository.projection.InstallmentSummaryProjection;
import sp26.group3.computer.sba301_computershop.enums.PaymentStatus;

import java.time.LocalDate;
import java.util.List;

import java.util.Optional;

@Repository
public interface OrderPaymentScheduleRepository extends JpaRepository<OrderPaymentSchedule, Integer> {
    List<OrderPaymentSchedule> findByOrderIdOrderByInstallmentNoAsc(int orderId);

    Optional<OrderPaymentSchedule> findByOrderIdAndInstallmentNo(int orderId, int installmentNo);

    // Task 1: Find records UNPAID and due exactly in 3 days
    List<OrderPaymentSchedule> findByStatusAndDueDate(PaymentStatus status, LocalDate dueDate);

    // Task 2: Find records UNPAID and overdue (dueDate < current date)
    List<OrderPaymentSchedule> findByStatusAndDueDateBefore(PaymentStatus status, LocalDate currentDate);

    // Task 3: Find records that are already OVERDUE
    List<OrderPaymentSchedule> findByStatus(PaymentStatus status);

    // Order giờ thuộc order-service (DB khác) — payment_mode/total_amount đã
    // được denormalize thẳng lên order_payment_schedule lúc tạo lịch, nên
    // không cần JOIN orders nữa.
    @Query(value = "SELECT " +
                   "SUM(CASE WHEN status = 'PAID' THEN amount ELSE 0 END) as totalPaid, " +
                   "SUM(CASE WHEN status = 'UNPAID' THEN amount ELSE 0 END) as totalUnpaid, " +
                   "SUM(CASE WHEN status = 'OVERDUE' THEN amount ELSE 0 END) as totalOverdue " +
                   "FROM order_payment_schedule " +
                   "WHERE payment_mode = 'INSTALLMENT'",
           nativeQuery = true)
    InstallmentSummaryProjection getInstallmentSummary();

    @Query(value = "SELECT order_id as orderId, user_id as userId, " +
                   "total_amount as orderTotal, " +
                   "COUNT(payment_schedule_id) as totalInstallments, " +
                   "SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) as paidInstallments, " +
                   "CONVERT(NVARCHAR, MIN(CASE WHEN status IN ('UNPAID', 'OVERDUE') THEN due_date ELSE NULL END), 23) as nextDueDate " +
                   "FROM order_payment_schedule " +
                   "WHERE payment_mode = 'INSTALLMENT' " +
                   "GROUP BY order_id, user_id, total_amount " +
                   "ORDER BY order_id DESC",
           nativeQuery = true)
    List<InstallmentOrderProjection> findInstallmentOrderDetails();
}
