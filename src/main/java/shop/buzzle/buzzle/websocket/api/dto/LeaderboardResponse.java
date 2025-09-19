package shop.buzzle.buzzle.websocket.api.dto;

import java.util.Map;

public record LeaderboardResponse(
        String type,
        String currentLeader,
        String currentLeaderEmail,
        Map<String, Integer> scores,
        Map<String, String> emailToName
) {
    public static LeaderboardResponse of(String currentLeaderEmail, String currentLeader, Map<String, Integer> scores, Map<String, String> emailToName) {
        return new LeaderboardResponse("LEADERBOARD", currentLeader, currentLeaderEmail, scores, emailToName);
    }
}