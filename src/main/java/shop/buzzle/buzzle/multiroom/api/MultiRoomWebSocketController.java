package shop.buzzle.buzzle.multiroom.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomCreateReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomJoinReqDto;
import shop.buzzle.buzzle.multiroom.application.MultiRoomWebSocketService;
import shop.buzzle.buzzle.websocket.api.dto.AnswerRequest;

@Controller
@RequiredArgsConstructor
public class MultiRoomWebSocketController {

    private final MultiRoomWebSocketService multiRoomWebSocketService;

    // 방 생성 (웹소켓)
    @MessageMapping("/room/create")
    public void createRoom(
            SimpMessageHeaderAccessor headerAccessor,
            @Payload MultiRoomCreateReqDto request
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.createAndJoinRoom(userEmail, request, headerAccessor);
    }

    // 초대코드로 방 참가 (웹소켓)
    @MessageMapping("/room/join")
    public void joinRoom(
            SimpMessageHeaderAccessor headerAccessor,
            @Payload MultiRoomJoinReqDto request
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.joinRoom(userEmail, request, headerAccessor);
    }

    // 방 나가기 (웹소켓)
    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.leaveRoom(roomId, userEmail);
    }

    // 게임 시작 (웹소켓)
    @MessageMapping("/room/{roomId}/start")
    public void startGame(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.startGame(roomId, userEmail);
    }

    // 답변 제출 (웹소켓)
    @MessageMapping("/room/{roomId}/answer")
    public void submitAnswer(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            @Payload AnswerRequest answerRequest
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.receiveMultiRoomAnswer(roomId, userEmail, answerRequest);
    }

    // 재연결 (웹소켓)
    @MessageMapping("/room/{roomId}/reconnect")
    public void handleReconnect(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        multiRoomWebSocketService.resendCurrentQuestionToUser(roomId);
    }
}