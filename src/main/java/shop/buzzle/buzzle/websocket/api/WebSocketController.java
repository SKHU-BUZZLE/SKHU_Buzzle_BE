package shop.buzzle.buzzle.websocket.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.buzzle.buzzle.websocket.application.WebSocketService;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;

    // 초대 퀴즈 대결용 구독 경로
    @MessageMapping("/room/{roomId}")
    public void invite(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        webSocketService.sendMessage(headerAccessor, roomId, message);
    }

    // 랜덤 매칭 퀴즈 대결용 구독 경로
    @MessageMapping("/game/{roomId}")
    public void random(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        String username = webSocketService.getUsernameFromSession(headerAccessor);

        webSocketService.sendGameMessage(roomId, username, message);
    }
}
