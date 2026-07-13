package sp26.group3.computer.sba301_computershop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "chat_conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"messages"})
@EqualsAndHashCode(of = "id")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_key", nullable = false, unique = true)
    private String roomKey;

    // User giờ thuộc user-service (DB riêng) — chỉ giữ id thô, không còn JPA relationship.
    @Column(name = "user1_id", nullable = false)
    private int user1Id;

    @Column(name = "user2_id", nullable = false)
    private int user2Id;

    @Column(name = "last_message", columnDefinition = "NVARCHAR(MAX)")
    private String lastMessage;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    private List<ChatMessage> messages;
}
