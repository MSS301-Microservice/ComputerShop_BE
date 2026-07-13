package sp26.group3.computer.sba301_computershop.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sp26.group3.computer.sba301_computershop.client.UserServiceClient;
import sp26.group3.computer.sba301_computershop.dto.request.ChatMessageRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ChatMessageResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ConversationResponse;
import sp26.group3.computer.sba301_computershop.dto.response.UserSummaryResponse;
import sp26.group3.computer.sba301_computershop.entity.ChatConversation;
import sp26.group3.computer.sba301_computershop.entity.ChatMessage;
import sp26.group3.computer.sba301_computershop.repository.ChatConversationRepository;
import sp26.group3.computer.sba301_computershop.repository.ChatMessageRepository;
import sp26.group3.computer.sba301_computershop.service.ChatService;
import sp26.group3.computer.sba301_computershop.util.SecurityUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

        private final ChatConversationRepository conversationRepo;
        private final ChatMessageRepository messageRepo;
        private final UserServiceClient userServiceClient;
        private final SimpMessagingTemplate messagingTemplate;

        private String buildRoomKey(int id1, int id2) {
                return Math.min(id1, id2) + "_" + Math.max(id1, id2);
        }

        @Override
        @Transactional(readOnly = true)
        public List<ConversationResponse> getConversations() {
                int myId = SecurityUtils.getCurrentUserId();
                return conversationRepo.findAllByUserId(myId).stream()
                                .map(conv -> {
                                        int otherId = conv.getUser1Id() == myId ? conv.getUser2Id() : conv.getUser1Id();
                                        UserSummaryResponse other = userServiceClient.getUser(otherId);
                                        long unread = messageRepo.countUnread(conv.getId(), myId);
                                        return ConversationResponse.builder()
                                                        .id(conv.getId())
                                                        .roomKey(conv.getRoomKey())
                                                        .otherUserId(otherId)
                                                        .otherUserName(other != null ? other.getUsername() : null)
                                                        .otherUserRole(other != null ? other.getRoleName() : null)
                                                        .lastMessage(conv.getLastMessage())
                                                        .lastMessageAt(conv.getLastMessageAt())
                                                        .unreadCount(unread)
                                                        .build();
                                })
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional(readOnly = true)
        public List<ChatMessageResponse> getChatHistory(int otherUserId) {
                int myId = SecurityUtils.getCurrentUserId();
                String roomKey = buildRoomKey(myId, otherUserId);
                return conversationRepo.findByRoomKey(roomKey)
                                .map(conv -> {
                                        Long convId = conv.getId();
                                        return messageRepo.findByConversationIdOrderBySentAtAsc(convId).stream()
                                                        .map(this::toResponseWithConvId)
                                                        .collect(Collectors.toList());
                                })
                                .orElse(List.of());
        }

        @Override
        @Transactional
        public void markAsRead(int otherUserId) {
                int myId = SecurityUtils.getCurrentUserId();
                String roomKey = buildRoomKey(myId, otherUserId);
                conversationRepo.findByRoomKey(roomKey)
                                .ifPresent(conv -> messageRepo.markAllAsRead(conv.getId(), myId));
        }

        @Override
        @Transactional
        public ChatMessageResponse sendMessage(ChatMessageRequest request, String senderEmail) {
                // Luồng WebSocket/STOMP: Principal chỉ mang email (không có claim userId
                // đầy đủ như JwtAuthenticationToken của REST), nên phải tra cứu qua user-service.
                UserSummaryResponse sender = userServiceClient.getUserByEmail(senderEmail);
                if (sender == null) {
                        throw new RuntimeException("Sender not found");
                }
                UserSummaryResponse receiver = userServiceClient.getUser(request.getReceiverId());
                if (receiver == null) {
                        throw new RuntimeException("Receiver not found");
                }

                String roomKey = buildRoomKey(sender.getUserId(), receiver.getUserId());

                ChatConversation conv = conversationRepo.findByRoomKey(roomKey)
                                .orElseGet(() -> conversationRepo.save(
                                                ChatConversation.builder()
                                                                .roomKey(roomKey)
                                                                .user1Id(Math.min(sender.getUserId(), receiver.getUserId()))
                                                                .user2Id(Math.max(sender.getUserId(), receiver.getUserId()))
                                                                .lastMessageAt(LocalDateTime.now())
                                                                .build()));

                ChatMessage msg = messageRepo.save(ChatMessage.builder()
                                .conversation(conv)
                                .senderId(sender.getUserId())
                                .content(request.getContent())
                                .sentAt(LocalDateTime.now())
                                .isRead(false)
                                .build());

                conv.setLastMessage(request.getContent());
                conv.setLastMessageAt(msg.getSentAt());
                conversationRepo.save(conv);

                ChatMessageResponse response = ChatMessageResponse.builder()
                                .id(msg.getId())
                                .conversationId(conv.getId())
                                .senderId(sender.getUserId())
                                .senderName(sender.getUsername())
                                .content(msg.getContent())
                                .sentAt(msg.getSentAt())
                                .isRead(msg.isRead())
                                .build();

                // Broadcast to WebSocket topic
                messagingTemplate.convertAndSend("/topic/chat." + roomKey, response);

                return response;
        }

        private ChatMessageResponse toResponseWithConvId(ChatMessage msg) {
                UserSummaryResponse sender = userServiceClient.getUser(msg.getSenderId());
                return ChatMessageResponse.builder()
                                .id(msg.getId())
                                .conversationId(msg.getConversation().getId())
                                .senderId(msg.getSenderId())
                                .senderName(sender != null ? sender.getUsername() : null)
                                .content(msg.getContent())
                                .sentAt(msg.getSentAt())
                                .isRead(msg.isRead())
                                .build();
        }
}
