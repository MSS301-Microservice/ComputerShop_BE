package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private int orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ProductItem giờ thuộc catalog-service (DB riêng) — chỉ giữ id thô.
    @Column(name = "item_id", nullable = false, unique = true)
    private int itemId;

    // Snapshot tại thời điểm đặt hàng — tránh phải gọi catalog-service mỗi lần
    // hiển thị lịch sử đơn hàng cũ, và tránh lịch sử "đổi" khi sản phẩm bị sửa sau này.
    @Column(name = "variant_id")
    private int variantId;

    @Column(name = "sku")
    private String sku;

    @Column(name = "variant_name")
    private String variantName;

    @Column(name = "product_id")
    private int productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "serial_number")
    private String serialNumber;

    @Column(name = "warranty_months")
    private int warrantyMonths;

    private int quantity;

    @Column(name = "unit_price")
    private double unitPrice;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "shipping_address")
    private String shippingAddress; 
}

