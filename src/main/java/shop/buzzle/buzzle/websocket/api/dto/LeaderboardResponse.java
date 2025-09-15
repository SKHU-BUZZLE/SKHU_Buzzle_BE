package shop.buzzle.buzzle.websocket.api.dto;

import java.util.Map;

public record LeaderboardResponse(
        String type,
        String currentLeader,
        Map<String, Integer> scores
) {
    public static LeaderboardResponse of(String currentLeader, Map<String, Integer> scores) {
        return new LeaderboardResponse("LEADERBOARD", currentLeader, scores);
    }
}