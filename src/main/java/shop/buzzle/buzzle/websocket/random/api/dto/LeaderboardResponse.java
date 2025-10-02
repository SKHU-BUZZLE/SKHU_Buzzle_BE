package shop.buzzle.buzzle.websocket.random.api.dto;

import shop.buzzle.buzzle.websocket.dto.MessageType;

import java.util.Map;

public record LeaderboardResponse(
        MessageType type,
        String currentLeader,
        String currentLeaderEmail,
        Map<String, Integer> scores,
        Map<String, String> emailToName
) {
    public static LeaderboardResponse of(String currentLeaderEmail, String currentLeader, Map<String, Integer> scores, Map<String, String> emailToName) {
        return new LeaderboardResponse(MessageType.LEADERBOARD, currentLeader, currentLeaderEmail, scores, emailToName);
    }
}