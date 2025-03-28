package shop.itcontest17.itcontest17.websocket.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.itcontest17.itcontest17.websocket.api.dto.AnswerRequest;
import shop.itcontest17.itcontest17.websocket.application.WSRoomService;
import shop.itcontest17.itcontest17.websocket.application.WebSocketService;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketService webSocketService;
    private final WSRoomService wsRoomService;

    // 보내는 경로 예시) /app/room/1
    @MessageMapping("/room/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public void processMessage(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        webSocketService.sendMessage(headerAccessor, roomId, message);
    }

    // 유저가 보내는 메세지 가공.
    @MessageMapping("/game/{roomId}")
    @SendTo("/topic/game/{roomId}")
    public void processGameMessage(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        String username = webSocketService.getUsernameFromSession(headerAccessor);

        webSocketService.sendGameMessage(roomId, username, message);
    }

    @MessageMapping("/game/{roomId}/answer")
    public void receiveAnswer(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            @Payload AnswerRequest answerRequest
    ) {
        String username = (String) headerAccessor.getSessionAttributes().get("userEmail");
        wsRoomService.receiveAnswer(roomId, username, answerRequest.index());
    }

}
