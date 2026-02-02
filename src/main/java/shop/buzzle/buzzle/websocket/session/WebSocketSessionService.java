package shop.buzzle.buzzle.websocket.session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.websocket.common.event.domain.UserDisconnectedEvent;
import shop.buzzle.buzzle.websocket.common.event.domain.UserSubscribedEvent;
import shop.buzzle.buzzle.websocket.invite.application.MultiRoomWebSocketService;
import shop.buzzle.buzzle.websocket.random.application.RandomRoomService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionService {

    private final RandomRoomService randomRoomService;
    private final MultiRoomWebSocketService inviteRoomService;
    private final MemberRepository memberRepository;
    private final Map<String, Set<String>> roomPlayers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> inviteRoomPlayers = new ConcurrentHashMap<>();
    private final Set<String> startedRooms = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleSubscription(UserSubscribedEvent event) {
        String destination = event.destination();
        String roomId = event.roomId();
        String userEmail = event.userEmail();

        if (destination.startsWith("/topic/room/")) {
            handleInviteRoomSubscribe(roomId, userEmail);
        } else if (destination.startsWith("/topic/game/")) {
            handleRegularRoomSubscribe(roomId, userEmail);
        }
    }

    @EventListener
    public void handleDisconnect(UserDisconnectedEvent event) {
        String destination = event.destination();
        String roomId = event.roomId();
        String userEmail = event.userEmail();

        if (destination.startsWith("/topic/room/")) {
            handleInviteRoomDisconnect(roomId, userEmail);
        } else if (destination.startsWith("/topic/game/")) {
            handleRegularRoomDisconnect(roomId, userEmail);
        }
    }

    private void handleRegularRoomSubscribe(String roomId, String userEmail) {
        roomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<String> players = roomPlayers.get(roomId);
        players.add(userEmail);

        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(MemberNotFoundException::new);

        log.info("🟢 {} 님이 일반방 {} 에 참가 (현재 인원: {})", member.getName(), roomId, players.size());

        randomRoomService.broadcastPlayerJoined(roomId, member.getEmail(), member.getName(), member.getPicture());

        if (players.size() == 2 && !startedRooms.contains(roomId)) {
            synchronized (startedRooms) {
                if (!startedRooms.contains(roomId)) {
                    startedRooms.add(roomId);
                    log.info("🚀 일반방 {} 게임 시작 조건 충족!", roomId);
                    randomRoomService.startGame(roomId, players.stream().toList());
                }
            }
        } else if (startedRooms.contains(roomId)) {
            log.info("🔁 {} 님이 재접속 - 방 {} 현재 문제 재전송", userEmail, roomId);
            randomRoomService.resendCurrentQuestionToUser(roomId);
        }
    }

    private void handleInviteRoomSubscribe(String roomId, String userEmail) {
        inviteRoomPlayers.putIfAbsent(roomId, ConcurrentHashMap.newKeySet());
        Set<String> players = inviteRoomPlayers.get(roomId);
        players.add(userEmail);
        log.info("🟢 {} 님이 초대방 {} 에 구독 (현재 인원: {})", userEmail, roomId, players.size());
    }

    private void handleRegularRoomDisconnect(String roomId, String userEmail) {
        Set<String> players = roomPlayers.get(roomId);
        if (players != null) {
            players.remove(userEmail);
            log.info("🔴 {} 님이 일반방 {} 에서 퇴장 (남은 인원: {})", userEmail, roomId, players.size());
            randomRoomService.broadcastToRoom(roomId, "PLAYER_LEFT", userEmail + "님이 퇴장했습니다.");
            if (players.isEmpty()) {
                roomPlayers.remove(roomId);
                startedRooms.remove(roomId);
                randomRoomService.forceCleanupRoom(roomId);
            }
        }
    }

    private void handleInviteRoomDisconnect(String roomId, String userEmail) {
        Set<String> players = inviteRoomPlayers.get(roomId);
        if (players != null) {
            players.remove(userEmail);
            log.info("🔴 {} 님이 초대방 {} 에서 연결 해제 (남은 인원: {})", userEmail, roomId, players.size());
            if (players.isEmpty()) {
                inviteRoomPlayers.remove(roomId);
                inviteRoomService.forceCleanupRoom(roomId);
            }
        }
    }
}
