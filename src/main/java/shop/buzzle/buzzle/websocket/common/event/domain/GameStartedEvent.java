package shop.buzzle.buzzle.websocket.common.event.domain;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

import java.time.Instant;
import java.util.List;

public record GameStartedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        List<String> playerEmails,
        int totalQuestions,
        QuizCategory category
) implements GameEvent {

    public GameStartedEvent(String roomId, String inviteCode, GameType gameType,
                            List<String> playerEmails, int totalQuestions, QuizCategory category) {
        this(roomId, inviteCode, gameType, Instant.now(), playerEmails, totalQuestions, category);
    }
}
