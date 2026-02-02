package shop.buzzle.buzzle.websocket.common.event.domain;

import java.time.Instant;
import java.util.Map;

public record LeaderboardUpdatedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        String leaderEmail,
        String leaderName,
        Map<String, Integer> scores,
        Map<String, String> emailToNameMap
) implements GameEvent {

    public LeaderboardUpdatedEvent(String roomId, String inviteCode, GameType gameType,
                                   String leaderEmail, String leaderName,
                                   Map<String, Integer> scores, Map<String, String> emailToNameMap) {
        this(roomId, inviteCode, gameType, Instant.now(), leaderEmail, leaderName, scores, emailToNameMap);
    }
}
