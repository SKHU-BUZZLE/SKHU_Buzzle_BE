package shop.buzzle.buzzle.websocket.invite.api;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InvitedRoomJoinReqDto;
import shop.buzzle.buzzle.websocket.invite.application.MultiRoomWebSocketService;
import shop.buzzle.buzzle.websocket.dto.AnswerRequest;

@Controller
@RequiredArgsConstructor
public class InviteWebSocketController {

    private final MultiRoomWebSocketService multiRoomWebSocketService;

    // 초대코드로 방 참가
    @MessageMapping("/room/join")
    public void joinRoom(
            SimpMessageHeaderAccessor headerAccessor,
            @Payload InvitedRoomJoinReqDto request
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.joinRoom(userEmail, request, headerAccessor);
    }

    // 방 나가기
    @MessageMapping("/room/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.leaveRoom(roomId, userEmail);
    }

    // 게임 시작
    @MessageMapping("/room/{roomId}/start")
    public void startGame(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.startGame(roomId, userEmail);
    }

    // 답변 제출
    @MessageMapping("/room/{roomId}/answer")
    public void submitAnswer(
            @DestinationVariable String roomId,
            SimpMessageHeaderAccessor headerAccessor,
            @Payload AnswerRequest answerRequest
    ) {
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        multiRoomWebSocketService.receiveMultiRoomAnswer(roomId, userEmail, answerRequest);
    }

    // 재연결
    @MessageMapping("/room/{roomId}/reconnect")
    public void handleReconnect(
            @DestinationVariable String roomId
    ) {
        multiRoomWebSocketService.resendCurrentQuestionToUser(roomId);
    }
}