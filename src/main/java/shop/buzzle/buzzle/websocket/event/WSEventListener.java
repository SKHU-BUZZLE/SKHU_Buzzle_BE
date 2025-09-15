package shop.buzzle.buzzle.websocket.event;

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

import shop.buzzle.buzzle.websocket.application.WSRoomService;
import shop.buzzle.buzzle.websocket.api.dto.PlayerJoinedResponse;
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

        // 플레이어 정보 조회 및 브로드캐스팅
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(MemberNotFoundException::new);

        log.info("🟢 {} 님이 방 {} 에 참가 (현재 인원: {})", member.getName(), roomId, players.size());

        PlayerJoinedResponse playerInfo = PlayerJoinedResponse.of(
                userEmail,
                member.getName(),
                member.getPicture()
        );
        wsRoomService.broadcastPlayerJoined(roomId, playerInfo);

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
        // ✅ 게임이 이미 시작된 상태에서 유저가 재입장한 경우 → 문제 재전송
        else if (startedRooms.contains(roomId)) {
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
