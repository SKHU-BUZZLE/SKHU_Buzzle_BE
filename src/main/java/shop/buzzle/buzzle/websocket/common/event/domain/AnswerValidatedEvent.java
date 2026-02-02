package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public record AnswerValidatedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        String playerEmail,
        String playerName,
        int questionIndex,
        int selectedIndex,
        int correctIndex,
        boolean isCorrect,
        boolean wasFirstCorrect
) implements GameEvent {

    public AnswerValidatedEvent(String roomId, String inviteCode, GameType gameType,
                                String playerEmail, String playerName, int questionIndex,
                                int selectedIndex, int correctIndex, boolean isCorrect, boolean wasFirstCorrect) {
        this(roomId, inviteCode, gameType, Instant.now(), playerEmail, playerName,
                questionIndex, selectedIndex, correctIndex, isCorrect, wasFirstCorrect);
    }
}
