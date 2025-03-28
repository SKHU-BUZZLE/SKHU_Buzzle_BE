package shop.itcontest17.itcontest17.websocket.application;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import shop.itcontest17.itcontest17.websocket.api.dto.RoomResponse;

@Service
@RequiredArgsConstructor
public class WSRoomService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 방의 모든 참가자에게 게임 시작 메시지를 전송합니다.
     *
     * @param roomId 전송할 대상 방 ID
     */
    public void sendGameStart(String roomId) {
        RoomResponse response = RoomResponse.of("GAME_START", "게임을 시작합니다!");
        messagingTemplate.convertAndSend("/topic/game/" + roomId, response);
    }

    public void broadcastToRoom(String roomId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                new RoomResponse(type, message)
        );
    }
}
