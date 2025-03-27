package shop.itcontest17.itcontest17.websocket.application;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shop.itcontest17.itcontest17.websocket.api.dto.WebSocketResponse;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    // 핸드셰이크에서 Principal 객체에 회원 정보를 담으려고 했지만 Null 값이 넘어오는 문제를 아직 해결 X
    // 핸드셰이크 JWT 인증단계에서 attributes 에 키벨류로 담아뒀다
    public String getUsernameFromSession(SimpMessageHeaderAccessor headerAccessor) {
        return (String) headerAccessor.getSessionAttributes().get("userEmail");
    }

    public void sendMessage(
            SimpMessageHeaderAccessor headerAccessor, String roomId, String message) {
        String username = getUsernameFromSession(headerAccessor);

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId, WebSocketResponse.Send.of(username, message));
    }

    public void sendSystemMessageToUser(String userId, Long roomId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId, "/topic/room/" + roomId, WebSocketResponse.Send.of(userId, message));
    }

    public void sendGameSystemMessageToUser(String userId, String roomId, String message) {
        messagingTemplate.convertAndSendToUser(
                userId, "/topic/game/" + roomId, WebSocketResponse.Send.of("System", message));
    }

    public void sendEvent(String roomId, String event) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId, WebSocketResponse.SendEvent.of(event));
    }

    public void sendGameEvent(String roomId, String event) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId, WebSocketResponse.SendEvent.of(event));
    }

    public void sendSystemMessage(String roomId, String message) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId, WebSocketResponse.Send.of("SYSTEM", message));
    }

    public void sendGameSystemMessage(String roomId, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId, WebSocketResponse.Send.of("SYSTEM", message));
    }

    public void sendGameMessage(String roomId, String username, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId, WebSocketResponse.Send.of(username, message));
    }

    public void sendGameQuizMessage(String roomId, String type, String message) {

        if ("question".equals(type)) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId, WebSocketResponse.SendQuestion.of(message));
        }
        if ("answer".equals(type)) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId, WebSocketResponse.SendAnswer.of(message));
        }
        if ("hint".equals(type)) {
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId, WebSocketResponse.SendHint.of(message));
        }
    }

    public void changeSessionFlag(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();
        String flag = (String) session.get("changeRoomToGame");

        if (flag == null || "false".equals(flag)) flag = "true";
        else flag = "false";

        session.put("changeRoomToGame", flag);
    }

    public void changeSessionFlagFalse(SimpMessageHeaderAccessor headerAccessor) {
        Map<String, Object> session = headerAccessor.getSessionAttributes();

        session.put("changeRoomToGame", "false");
    }
}
