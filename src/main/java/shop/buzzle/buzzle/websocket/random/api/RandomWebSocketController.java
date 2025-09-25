package shop.buzzle.buzzle.websocket.random.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.buzzle.buzzle.websocket.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.random.application.RandomRoomService;
import shop.buzzle.buzzle.websocket.random.application.RandomWebSocketService;

@Controller
@RequiredArgsConstructor
public class RandomWebSocketController {

    private final RandomWebSocketService randomWebSocketService;
    private final RandomRoomService wsRoomService;

    // 보내는 경로 예시) /app/room/1
    @MessageMapping("/room/{roomId}")
    public void processMessage(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        randomWebSocketService.sendMessage(headerAccessor, roomId, message);
    }

    // 유저가 보내는 메세지 가공.
    @MessageMapping("/game/{roomId}")
    public void processGameMessage(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            String message) {
        String username = randomWebSocketService.getUsernameFromSession(headerAccessor);

        randomWebSocketService.sendGameMessage(roomId, username, message);
    }

    @MessageMapping("/game/{roomId}/answer")
    public void receiveAnswer(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            @Payload AnswerRequest answerRequest
    ) {
        String username = (String) headerAccessor.getSessionAttributes().get("userEmail");
        wsRoomService.receiveAnswer(roomId, username, answerRequest);
    }
}
