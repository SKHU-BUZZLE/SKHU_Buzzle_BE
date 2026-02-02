package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;

public sealed interface GameEvent permits
        GameStartedEvent,
        QuestionSentEvent,
        AnswerValidatedEvent,
        TimerTickEvent,
        TimerExpiredEvent,
        LeaderboardUpdatedEvent,
        GameEndedEvent,
        PlayerJoinedEvent,
        PlayerLeftEvent,
        RoomNotificationEvent,
        NotificationEvent {

    String roomId();
    String inviteCode();
    GameType gameType();
    Instant timestamp();

    enum GameType {
        INVITE, RANDOM
    }
}
