package shop.buzzle.buzzle.multiroom.api.dto.response;

import java.util.List;

public record GameEndResponseDto(
        String type,
        String message,
        GameEndData data
) {
    public static GameEndResponseDto of(List<PlayerRanking> rankings, boolean hasTie) {
        String message;
        if (hasTie) {
            message = "게임이 종료되었습니다! 동점자가 있습니다. 방이 해체됩니다.";
        } else {
            PlayerRanking winner = rankings.get(0);
            message = "게임이 종료되었습니다! 우승자: " + winner.name() + ". 방이 해체됩니다.";
        }

        return new GameEndResponseDto(
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