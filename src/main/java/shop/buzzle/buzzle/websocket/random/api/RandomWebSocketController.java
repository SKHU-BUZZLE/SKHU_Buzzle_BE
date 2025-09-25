package shop.buzzle.buzzle.websocket.random.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.buzzle.buzzle.websocket.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.random.application.RandomRoomService;

@Controller
@RequiredArgsConstructor
public class RandomWebSocketController {

    private final RandomRoomService wsRoomService;

    // 정답 받기
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
