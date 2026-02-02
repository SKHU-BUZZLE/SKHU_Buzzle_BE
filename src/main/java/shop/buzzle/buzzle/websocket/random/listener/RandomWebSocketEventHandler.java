package shop.buzzle.buzzle.websocket.random.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import shop.buzzle.buzzle.websocket.common.event.domain.*;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
import shop.buzzle.buzzle.websocket.common.messaging.WebSocketMessageDispatcher;
import shop.buzzle.buzzle.websocket.dto.AnswerResponse;

import java.util.Map;

/**
 * 랜덤 매칭방(Random) 게임 이벤트를 WebSocket 메시지로 변환하여 전송하는 핸들러.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RandomWebSocketEventHandler {

    private final WebSocketMessageDispatcher dispatcher;

    @EventListener
    public void onGameStarted(GameStartedEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] GameStarted - Room: {}, Players: {}, Questions: {}",
                event.roomId(), event.playerEmails().size(), event.totalQuestions());

        Map<String, Object> payload = Map.of(
                "type", "GAME_START",
                "totalQuestions", event.totalQuestions(),
                "countdownSeconds", 0
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }

    @EventListener
    public void onQuestionSent(QuestionSentEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] QuestionSent - Room: {}, Question: {}/{}",
                event.roomId(), event.questionIndex() + 1, "?");

        Map<String, Object> payload = Map.of(
                "type", "QUESTION",
                "question", event.questionText(),
                "options", event.options(),
                "questionIndex", event.questionIndex()
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }

    @EventListener
    public void onTimerTick(TimerTickEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        Map<String, Object> payload = Map.of(
                "type", "TIMER",
                "remainingTime", event.remainingSeconds()
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }

    @EventListener
    public void onTimerExpired(TimerExpiredEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] TimerExpired - Room: {}, Question: {}",
                event.roomId(), event.questionIndex());

        Map<String, Object> payload = Map.of(
                "type", "TIME_UP",
                "message", "시간이 종료되었습니다!"
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }

    @EventListener
    public void onAnswerValidated(AnswerValidatedEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] AnswerValidated - Room: {}, Player: {}, Correct: {}",
                event.roomId(), event.playerName(), event.isCorrect());

        AnswerResponse response = AnswerResponse.of(
                event.playerEmail(),
                event.playerName(),
                event.isCorrect(),
                String.valueOf(event.correctIndex()),
                String.valueOf(event.selectedIndex())
        );
        dispatcher.sendToGameRoom(event.roomId(), response);
    }

    @EventListener
    public void onLeaderboardUpdated(LeaderboardUpdatedEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.debug("[EVENT_HANDLER-RANDOM] LeaderboardUpdated - Room: {}, Leader: {}",
                event.roomId(), event.leaderName());

        Map<String, Object> payload = Map.of(
                "type", "LEADERBOARD",
                "currentLeader", event.leaderName() != null ? event.leaderName() : "",
                "currentLeaderEmail", event.leaderEmail() != null ? event.leaderEmail() : "",
                "scores", event.scores(),
                "emailToName", event.emailToNameMap()
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }

    @EventListener
    public void onGameEnded(GameEndedEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] GameEnded - Room: {}, Winner: {}, Tie: {}",
                event.roomId(), event.winnerEmail(), event.hasTie());

        // Note: The payload for random game end might differ. This is a placeholder.
        // The original `GameEndResponse.withRanking` needs to be adapted.
        dispatcher.sendToGameRoom(event.roomId(), event.gameEndData());
    }

    @EventListener
    public void onPlayerJoined(PlayerJoinedEvent event) {
        if (event.gameType() != GameType.RANDOM) return;

        log.info("[EVENT_HANDLER-RANDOM] PlayerJoined - Room: {}, Player: {}",
                event.roomId(), event.playerName());

        Map<String, Object> payload = Map.of(
                "type", "PLAYER_JOINED",
                "email", event.playerEmail(),
                "name", event.playerName(),
                "picture", event.playerPicture() != null ? event.playerPicture() : ""
        );
        dispatcher.sendToGameRoom(event.roomId(), payload);
    }
}
