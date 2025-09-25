package shop.buzzle.buzzle.websocket.global.event;

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

import shop.buzzle.buzzle.websocket.random.application.WSRoomService;
import shop.buzzle.buzzle.websocket.random.api.dto.PlayerJoinedResponse;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSEventListener {

    private final WSRoomService wsRoomService;
    private final MemberRepository memberRepository;
    private final Map<String, Set<String>> roomPlayers = new ConcurrentHashMap<>();
    private final Set<String> startedRooms = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String destination = accessor.getDestination();
        String userEmail = (String) sessionAttributes.get("userEmail");

        // ✅ 1. 개인 큐(/user/**) 구독은 무시
        if (destination == null || destination.startsWith("/user/")) {
            return;
        }

        // ✅ 2. 멀티룸 구독 (/topic/room/{roomId})
        if (destination.startsWith("/topic/room/")) {
            String roomId = parseRoomId(destination);
            if (roomId != null && userEmail != null) {
                sessionAttributes.put("roomId", roomId);
                sessionAttributes.put("destination", destination);
                log.info("🟢 {} 님이 멀티룸 {} 에 구독", userEmail, roomId);
            }
            return;
        }

        // ✅ 3. 일반방 구독 (/topic/game/{roomId})
        if (destination.startsWith("/topic/game/")) {
            String roomId = parseRoomId(destination);
            if (roomId != null && userEmail != null) {
                sessionAttributes.put("roomId", roomId);
                sessionAttributes.put("destination", destination);
                handleRegularRoomSubscribe(roomId, userEmail);
            }
        }
    }

    private void handleRegularRoomSubscribe(String roomId, String userEmail) {
        roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<String> players = roomPlayers.get(roomId);
        players.add(userEmail);

        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(MemberNotFoundException::new);

        log.info("🟢 {} 님이 일반방 {} 에 참가 (현재 인원: {})", member.getName(), roomId, players.size());

        PlayerJoinedResponse playerInfo = PlayerJoinedResponse.of(
                userEmail,
                member.getName(),
                member.getPicture()
        );
        wsRoomService.broadcastPlayerJoined(roomId, playerInfo);

        if (players.size() == 2 && !startedRooms.contains(roomId)) {
            synchronized (startedRooms) {
                if (!startedRooms.contains(roomId)) {
                    startedRooms.add(roomId);
                    log.info("🚀 일반방 {} 게임 시작 조건 충족!", roomId);
                    wsRoomService.startGame(roomId, players.stream().toList());
                }
            }
        } else if (startedRooms.contains(roomId)) {
            log.info("🔁 {} 님이 재접속 - 방 {} 현재 문제 재전송", userEmail, roomId);
            wsRoomService.resendCurrentQuestionToUser(roomId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String roomId = (String) sessionAttributes.get("roomId");
        String userEmail = (String) sessionAttributes.get("userEmail");
        String destination = (String) sessionAttributes.get("destination");

        if (roomId != null && userEmail != null) {
            if (destination != null && destination.startsWith("/topic/room/")) {
                log.info("🔴 {} 님이 멀티룸 {} 에서 연결 해제", userEmail, roomId);
            } else if (destination != null && destination.startsWith("/topic/game/")) {
                handleRegularRoomDisconnect(roomId, userEmail);
            }
        }
    }

    private void handleRegularRoomDisconnect(String roomId, String userEmail) {
        Set<String> players = roomPlayers.get(roomId);
        if (players != null) {
            players.remove(userEmail);
            log.info("🔴 {} 님이 일반방 {} 에서 퇴장 (남은 인원: {})", userEmail, roomId, players.size());
            wsRoomService.broadcastToRoom(roomId, "PLAYER_LEFT", userEmail + "님이 퇴장했습니다.");
            if (players.isEmpty()) {
                roomPlayers.remove(roomId);
                startedRooms.remove(roomId);
            }
        }
    }

    private String parseRoomId(String destination) {
        String[] parts = destination.split("/");
        return parts.length > 0 ? parts[parts.length - 1] : null;
    }
}
