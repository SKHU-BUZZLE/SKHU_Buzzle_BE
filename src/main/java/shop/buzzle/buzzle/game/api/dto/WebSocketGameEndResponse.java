package shop.buzzle.buzzle.game.api.dto;

public record WebSocketGameEndResponse(
        String type,
        String message,
        String winner,
        String winnerEmail
) {
    public static WebSocketGameEndResponse of(String winnerEmail, String winner) {
        return new WebSocketGameEndResponse("GAME_END", "게임이 종료되었습니다.", winner, winnerEmail);
    }
}
