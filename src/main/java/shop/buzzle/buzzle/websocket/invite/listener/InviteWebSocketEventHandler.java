package shop.buzzle.buzzle.websocket.invite.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import shop.buzzle.buzzle.websocket.common.event.domain.*;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
import shop.buzzle.buzzle.websocket.common.messaging.WebSocketMessageDispatcher;
import shop.buzzle.buzzle.websocket.dto.AnswerResponse;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.invitedRoomEventResDto;

import java.util.Map;

/**
 * 초대방(Invite) 게임 이벤트를 WebSocket 메시지로 변환하여 전송하는 핸들러.
 * 게임 비즈니스 로직과 WebSocket 통신 계층을 분리하여 테스트 용이성을 확보합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InviteWebSocketEventHandler {

    private final WebSocketMessageDispatcher dispatcher;

    @EventListener
    public void onGameStarted(GameStartedEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] GameStarted - Room: {}, Players: {}, Questions: {}",
                event.inviteCode(), event.playerEmails().size(), event.totalQuestions());

        Map<String, Object> payload = Map.of(
                "type", "GAME_START",
                "totalQuestions", event.totalQuestions(),
                "countdownSeconds", 0
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onQuestionSent(QuestionSentEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] QuestionSent - Room: {}, Question: {}/{}",
                event.inviteCode(), event.questionIndex() + 1, "?");

        Map<String, Object> payload = Map.of(
                "type", "QUESTION",
                "question", event.questionText(),
                "options", event.options(),
                "questionIndex", event.questionIndex()
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onTimerTick(TimerTickEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        Map<String, Object> payload = Map.of(
                "type", "TIMER",
                "remainingTime", event.remainingSeconds()
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onTimerExpired(TimerExpiredEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] TimerExpired - Room: {}, Question: {}",
                event.inviteCode(), event.questionIndex());

        Map<String, Object> payload = Map.of(
                "type", "TIME_UP",
                "message", "시간이 종료되었습니다!"
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onAnswerValidated(AnswerValidatedEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] AnswerValidated - Room: {}, Player: {}, Correct: {}",
                event.inviteCode(), event.playerName(), event.isCorrect());

        AnswerResponse response = AnswerResponse.of(
                event.playerEmail(),
                event.playerName(),
                event.isCorrect(),
                String.valueOf(event.correctIndex()),
                String.valueOf(event.selectedIndex())
        );
        dispatcher.sendToRoom(event.inviteCode(), response);
    }

    @EventListener
    public void onLeaderboardUpdated(LeaderboardUpdatedEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.debug("[EVENT_HANDLER] LeaderboardUpdated - Room: {}, Leader: {}",
                event.inviteCode(), event.leaderName());

        Map<String, Object> payload = Map.of(
                "type", "LEADERBOARD",
                "currentLeader", event.leaderName() != null ? event.leaderName() : "",
                "currentLeaderEmail", event.leaderEmail() != null ? event.leaderEmail() : "",
                "scores", event.scores(),
                "emailToName", event.emailToNameMap()
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onGameEnded(GameEndedEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] GameEnded - Room: {}, Winner: {}, Tie: {}",
                event.inviteCode(), event.winnerEmail(), event.hasTie());

        invitedRoomEventResDto response = invitedRoomEventResDto.gameEndWithRanking(event.gameEndData());
        dispatcher.sendToRoom(event.inviteCode(), response);
    }

    @EventListener
    public void onPlayerJoined(PlayerJoinedEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] PlayerJoined - Room: {}, Player: {}",
                event.inviteCode(), event.playerName());

        // 기존 invitedRoomEventResDto.playerJoined()는 Member 객체를 받으므로
        // 별도 DTO를 만들거나 기존 로직 유지
        Map<String, Object> payload = Map.of(
                "type", "PLAYER_JOINED",
                "email", event.playerEmail(),
                "name", event.playerName(),
                "picture", event.playerPicture() != null ? event.playerPicture() : ""
        );
        dispatcher.sendToRoom(event.inviteCode(), payload);
    }

    @EventListener
    public void onPlayerLeft(PlayerLeftEvent event) {
        if (event.gameType() != GameType.INVITE) return;

        log.info("[EVENT_HANDLER] PlayerLeft - Room: {}, Player: {}, WasHost: {}",
                event.inviteCode(), event.playerName(), event.wasHost());

        if (event.wasHost()) {
            Map<String, Object> payload = Map.of(
                    "type", "ROOM_DISBANDED",
                    "message", "방장이 퇴장하여 방이 해체되었습니다."
            );
            dispatcher.sendToRoom(event.inviteCode(), payload);
        } else {
            Map<String, Object> payload = Map.of(
                    "type", "PLAYER_LEFT",
                    "email", event.playerEmail(),
                    "name", event.playerName()
            );
            dispatcher.sendToRoom(event.inviteCode(), payload);
        }
    }
}
