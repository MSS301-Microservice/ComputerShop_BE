package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private int cartItemId;

    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // ProductVariant giờ thuộc catalog-service (DB riêng) — chỉ giữ id thô.
    @Column(name = "variant_id", nullable = false)
    private int variantId;

    @Column(nullable = false)
    private int quantity;
}
