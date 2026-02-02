package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public record TimerExpiredEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        int questionIndex
) implements GameEvent {

    public TimerExpiredEvent(String roomId, String inviteCode, GameType gameType, int questionIndex) {
        this(roomId, inviteCode, gameType, Instant.now(), questionIndex);
    }
}
