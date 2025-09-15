package shop.buzzle.buzzle.websocket.api.dto;

import java.util.Map;

public record LeaderboardResponse(
        String currentLeader,
        Map<String, Integer> scores,
        String type
) {
    public static LeaderboardResponse of(String currentLeader, Map<String, Integer> scores) {
        return new LeaderboardResponse(currentLeader, scores, "leaderboard");
    }
}