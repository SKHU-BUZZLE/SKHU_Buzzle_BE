package shop.itcontest17.itcontest17.websocket.event;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import shop.itcontest17.itcontest17.websocket.application.WSRoomService;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSEventListener {

    private final WSRoomService wsRoomService;
    private final Map<String, Set<String>> roomPlayers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String roomId = parseRoomId(destination);
        String userEmail = (String) sessionAttributes.get("userEmail");

        if (roomId == null || userEmail == null) {
            log.warn("roomId 또는 userEmail 누락: roomId={}, userEmail={}", roomId, userEmail);
            return;
        }

        sessionAttributes.put("roomId", roomId);

        roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<String> players = roomPlayers.get(roomId);
        players.add(userEmail);

        log.info("🟢 {} 님이 방 {} 에 참가 (현재 인원: {})", userEmail, roomId, players.size());

        // ✅ 브로드캐스팅: 참가자 입장
        wsRoomService.broadcastToRoom(roomId, "PLAYER_JOINED", userEmail + "님이 입장했습니다.");

        // ✅ 게임 시작 조건 확인
        if (players.size() == 2) {
            log.info("🚀 방 {} 게임 시작 조건 충족!", roomId);
            wsRoomService.sendGameStart(roomId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String roomId = (String) sessionAttributes.get("roomId");
        String userEmail = (String) sessionAttributes.get("userEmail");

        if (roomId != null && userEmail != null) {
            Set<String> players = roomPlayers.get(roomId);
            if (players != null) {
                players.remove(userEmail);
                log.info("🔴 {} 님이 방 {} 에서 퇴장 (남은 인원: {})", userEmail, roomId, players.size());

                // ✅ 브로드캐스팅: 퇴장
                wsRoomService.broadcastToRoom(roomId, "PLAYER_LEFT", userEmail + "님이 퇴장했습니다.");

                if (players.isEmpty()) {
                    roomPlayers.remove(roomId);
                }
            }
        }
    }

    private String parseRoomId(String destination) {
        String[] parts = destination.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
