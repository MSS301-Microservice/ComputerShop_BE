package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sp26.group3.computer.sba301_computershop.enums.ComponentType;

@Entity
@Table(name = "pc_build_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PCBuildItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "build_item_id")
    private int buildItemId;

    @ManyToOne
    @JoinColumn(name = "build_id", nullable = false)
    private PCBuild build;

    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, length = 50)
    private ComponentType componentType;

    // ProductVariant giờ thuộc catalog-service (DB riêng) — chỉ giữ id thô.
    @Column(name = "variant_id", nullable = false)
    private int variantId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double price;
}

