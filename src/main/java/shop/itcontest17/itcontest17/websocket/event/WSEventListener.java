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
    private final Set<String> startedRooms = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String destination = accessor.getDestination();
        String roomId = parseRoomId(destination);
        String userEmail = (String) sessionAttributes.get("userEmail");

        if (roomId == null || userEmail == null) {
            log.warn("roomId 또는 userEmail 누락: roomId={}, userEmail={}", roomId, userEmail);
            return;
        }

        sessionAttributes.put("roomId", roomId);

        // 참가자 목록 갱신
        roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<String> players = roomPlayers.get(roomId);
        players.add(userEmail);

        log.info("🟢 {} 님이 방 {} 에 참가 (현재 인원: {})", userEmail, roomId, players.size());
        wsRoomService.broadcastToRoom(roomId, "PLAYER_JOINED", userEmail + "님이 입장했습니다.");

        // ✅ 2명이 모이면 게임 시작 (단, 이미 시작되지 않았다면)
        if (players.size() == 2 && !startedRooms.contains(roomId)) {
            synchronized (startedRooms) {
                if (!startedRooms.contains(roomId)) {
                    startedRooms.add(roomId);
                    log.info("🚀 방 {} 게임 시작 조건 충족!", roomId);
                    wsRoomService.startGame(roomId);
                }
            }
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
                wsRoomService.broadcastToRoom(roomId, "PLAYER_LEFT", userEmail + "님이 퇴장했습니다.");
                if (players.isEmpty()) {
                    roomPlayers.remove(roomId);
                    startedRooms.remove(roomId); // ✅ 방 비면 초기화
                }
            }
        }
    }

    private String parseRoomId(String destination) {
        String[] parts = destination.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
