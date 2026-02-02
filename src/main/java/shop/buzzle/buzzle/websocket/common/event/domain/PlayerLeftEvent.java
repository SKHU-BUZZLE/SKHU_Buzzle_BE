package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public record PlayerLeftEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        String playerEmail,
        String playerName,
        boolean wasHost
) implements GameEvent {

    public PlayerLeftEvent(String roomId, String inviteCode, GameType gameType,
                           String playerEmail, String playerName, boolean wasHost) {
        this(roomId, inviteCode, gameType, Instant.now(), playerEmail, playerName, wasHost);
    }
}
