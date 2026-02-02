package shop.buzzle.buzzle.websocket.common.event.domain;

import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;

import java.time.Instant;

public record NotificationEvent(
        String userEmail,
        String destination,
        Object payload,
        GameType gameType,
        Instant timestamp
) implements GameEvent {
    public NotificationEvent(String userEmail, String destination, Object payload, GameType gameType) {
        this(userEmail, destination, payload, gameType, Instant.now());
    }

    @Override
    public String roomId() {
        return null; // 개인 알림은 특정 방에 종속되지 않음
    }

    @Override
    public String inviteCode() {
        return null; // 개인 알림은 특정 방에 종속되지 않음
    }
}
