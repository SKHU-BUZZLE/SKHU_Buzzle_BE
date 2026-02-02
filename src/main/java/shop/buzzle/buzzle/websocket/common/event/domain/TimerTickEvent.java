package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public record TimerTickEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        int remainingSeconds
) implements GameEvent {

    public TimerTickEvent(String roomId, String inviteCode, GameType gameType, int remainingSeconds) {
        this(roomId, inviteCode, gameType, Instant.now(), remainingSeconds);
    }
}
