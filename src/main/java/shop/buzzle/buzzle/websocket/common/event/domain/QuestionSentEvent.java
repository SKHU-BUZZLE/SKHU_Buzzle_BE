package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;
import java.util.List;

public record QuestionSentEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        String questionText,
        List<String> options,
        int questionIndex
) implements GameEvent {

    public QuestionSentEvent(String roomId, String inviteCode, GameType gameType,
                             String questionText, List<String> options, int questionIndex) {
        this(roomId, inviteCode, gameType, Instant.now(), questionText, options, questionIndex);
    }
}
