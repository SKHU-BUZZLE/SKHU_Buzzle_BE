package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public record PlayerJoinedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        String playerEmail,
        String playerName,
        String playerPicture
) implements GameEvent {

    public PlayerJoinedEvent(String roomId, String inviteCode, GameType gameType,
                             String playerEmail, String playerName, String playerPicture) {
        this(roomId, inviteCode, gameType, Instant.now(), playerEmail, playerName, playerPicture);
    }
}
