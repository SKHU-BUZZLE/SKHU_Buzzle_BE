package shop.buzzle.buzzle.game.api.dto;

import java.util.List;
import java.util.Map;

public record WebSocketGameEndResponse(
        String type,
        String message,
        String winner,
        String winnerEmail,
        GameEndData data
) {
    public static WebSocketGameEndResponse of(String winnerEmail, String winner) {
        return new WebSocketGameEndResponse("GAME_END", "게임이 종료되었습니다.", winner, winnerEmail, null);
    }

    public static WebSocketGameEndResponse withRanking(List<PlayerRanking> rankings, boolean hasTie) {
        String winner = null;
        String winnerEmail = null;
        String message;

        if (hasTie) {
            message = "게임이 종료되었습니다! 동점자가 있습니다.";
        } else {
            PlayerRanking firstPlace = rankings.get(0);
            winner = firstPlace.name();
            winnerEmail = firstPlace.email();
            message = "게임이 종료되었습니다! 우승자: " + winner;
        }

        return new WebSocketGameEndResponse(
                "GAME_END_RANKING",
                message,
                winner,
                winnerEmail,
                new GameEndData(rankings, hasTie)
        );
    }

    public record GameEndData(
            List<PlayerRanking> rankings,
            boolean hasTie
    ) {}

    public record PlayerRanking(
            int rank,
            String email,
            String name,
            String picture,
            int score,
            boolean isWinner
    ) {}
}
