package sp26.group3.computer.sba301_computershop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sp26.group3.computer.sba301_computershop.entity.ChatConversation;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findByRoomKey(String roomKey);

    // User1/User2 giờ chỉ là id thô (user-service sở hữu bảng users) — không còn
    // JOIN FETCH được nữa; username/role của 2 phía được enrich qua UserServiceClient
    // ở tầng service.
    @Query("SELECT c FROM ChatConversation c WHERE c.user1Id = :userId OR c.user2Id = :userId ORDER BY c.lastMessageAt DESC")
    List<ChatConversation> findAllByUserId(@Param("userId") int userId);
}
