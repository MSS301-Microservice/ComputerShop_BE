package sp26.group3.computer.sba301_computershop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;
import sp26.group3.computer.sba301_computershop.dto.request.ChatMessageRequest;
import sp26.group3.computer.sba301_computershop.dto.response.ApiResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ChatMessageResponse;
import sp26.group3.computer.sba301_computershop.dto.response.ConversationResponse;
import sp26.group3.computer.sba301_computershop.dto.response.UserSummaryResponse;
import sp26.group3.computer.sba301_computershop.client.UserServiceClient;
import sp26.group3.computer.sba301_computershop.service.ChatService;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserServiceClient userServiceClient;

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(chatService.getConversations())
                .build();
    }

    @GetMapping("/history/{otherUserId}")
    public ApiResponse<List<ChatMessageResponse>> getChatHistory(@PathVariable int otherUserId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(chatService.getChatHistory(otherUserId))
                .build();
    }

    @PatchMapping("/read/{otherUserId}")
    public ApiResponse<Void> markAsRead(@PathVariable int otherUserId) {
        chatService.markAsRead(otherUserId);
        return new ApiResponse<>();
    }

    @GetMapping("/staff-list")
    public ApiResponse<List<Map<String, Object>>> getStaffList() {
        List<UserSummaryResponse> staffUsers = userServiceClient.getUsersByRole("STAFF");
        List<Map<String, Object>> result = staffUsers.stream()
                .map(u -> Map.<String, Object>of("userId", u.getUserId(), "username", u.getUsername()))
                .collect(Collectors.toList());
        return ApiResponse.<List<Map<String, Object>>>builder()
                .result(result)
                .build();
    }

    @MessageMapping("/chat.send")
    public void handleChatMessage(@Payload ChatMessageRequest request, Principal principal) {
        chatService.sendMessage(request, principal.getName());
    }
}
