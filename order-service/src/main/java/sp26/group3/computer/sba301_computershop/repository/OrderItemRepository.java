package sp26.group3.computer.sba301_computershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sp26.group3.computer.sba301_computershop.entity.OrderItem;
import sp26.group3.computer.sba301_computershop.repository.projection.ProductRevenueProjection;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrderOrderId(int orderId);

    // Product/ProductVariant giờ ở catalog-service (DB khác) — không JOIN được nữa.
    // Dùng product_name/variant_name snapshot lưu sẵn trên order_items lúc đặt hàng.
    @Query(value = "SELECT oi.product_name as productName, oi.variant_name as variantName, " +
                   "SUM(oi.quantity) as totalSold, SUM(oi.unit_price * oi.quantity) as revenue " +
                   "FROM order_items oi " +
                   "JOIN orders o ON oi.order_id = o.order_id " +
                   "WHERE o.status = 'DELIVERED' " +
                   "AND o.order_date >= :from AND o.order_date <= :to " +
                   "GROUP BY oi.product_name, oi.variant_name " +
                   "ORDER BY revenue DESC",
           nativeQuery = true)
    List<ProductRevenueProjection> findTopProducts(@Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to);
}
