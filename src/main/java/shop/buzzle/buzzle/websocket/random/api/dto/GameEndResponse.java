package shop.buzzle.buzzle.websocket.random.api.dto;

import java.util.List;

public record GameEndResponse(
        String type,
        String message,
        GameEndData data
) {
    public static GameEndResponse withRanking(List<PlayerRanking> rankings, boolean hasTie) {
        String message;

        if (hasTie) {
            message = "게임이 종료되었습니다! 동점자가 있습니다.";
        } else {
            PlayerRanking firstPlace = rankings.get(0);
            message = "게임이 종료되었습니다! 우승자: " + firstPlace.name();
        }

        return new GameEndResponse(
                "GAME_END_RANKING",
                message,
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
