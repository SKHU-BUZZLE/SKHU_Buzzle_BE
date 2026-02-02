package shop.buzzle.buzzle.websocket.common.event.domain;

import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;

import java.time.Instant;

public record RoomNotificationEvent(
        String roomId,
        String inviteCode,
        Object payload,
        GameType gameType,
        Instant timestamp
) implements GameEvent {
    public RoomNotificationEvent(String roomId, String inviteCode, Object payload, GameType gameType) {
        this(roomId, inviteCode, payload, gameType, Instant.now());
    }
}