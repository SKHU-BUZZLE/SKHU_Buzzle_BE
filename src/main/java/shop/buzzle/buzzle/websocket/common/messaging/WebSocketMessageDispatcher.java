package shop.buzzle.buzzle.websocket.common.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketMessageDispatcher {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 초대방(invite room)에 메시지 전송
     * @param inviteCode 초대 코드
     * @param payload 전송할 페이로드
     */
    public void sendToRoom(String inviteCode, Object payload) {
        log.debug("[WS_DISPATCH] Sending to /topic/room/{}: {}", inviteCode, payload.getClass().getSimpleName());
        messagingTemplate.convertAndSend("/topic/room/" + inviteCode, payload);
    }

    /**
     * 랜덤매칭 방(game room)에 메시지 전송
     * @param roomId 방 ID
     * @param payload 전송할 페이로드
     */
    public void sendToGameRoom(String roomId, Object payload) {
        log.debug("[WS_DISPATCH] Sending to /topic/game/{}: {}", roomId, payload.getClass().getSimpleName());
        messagingTemplate.convertAndSend("/topic/game/" + roomId, payload);
    }

    /**
     * 특정 사용자의 개인 큐에 메시지 전송
     * @param userEmail 사용자 이메일
     * @param destination 목적지 (예: "/queue/room")
     * @param payload 전송할 페이로드
     */
    public void sendToUser(String userEmail, String destination, Object payload) {
        log.debug("[WS_DISPATCH] Sending to user {}, destination {}: {}",
                userEmail, destination, payload.getClass().getSimpleName());
        messagingTemplate.convertAndSendToUser(userEmail, destination, payload);
    }
}
