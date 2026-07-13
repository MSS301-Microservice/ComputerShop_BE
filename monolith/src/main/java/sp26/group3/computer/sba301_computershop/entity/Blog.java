package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blog_id")
    private int blogId;

    // User giờ thuộc user-service (DB riêng) — chỉ giữ id thô, không còn JPA relationship.
    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;
}