package shop.buzzle.buzzle.websocket.application;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shop.buzzle.buzzle.websocket.api.dto.WebSocketResponse;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public String getUsernameFromSession(SimpMessageHeaderAccessor headerAccessor) {
        return (String) headerAccessor.getSessionAttributes().get("userEmail");
    }

    public void sendMessage(
            SimpMessageHeaderAccessor headerAccessor, String roomId, String message) {
        String username = getUsernameFromSession(headerAccessor);

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId, WebSocketResponse.Send.of(username, message));
    }

    public void sendGameMessage(String roomId, String username, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId, WebSocketResponse.Send.of(username, message));
    }
}
