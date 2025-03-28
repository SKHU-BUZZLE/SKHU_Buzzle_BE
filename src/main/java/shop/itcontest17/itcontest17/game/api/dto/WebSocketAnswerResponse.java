package shop.itcontest17.itcontest17.game.api.dto;

public record WebSocketAnswerResponse(
        String type, String message, String correctAnswer, boolean correct
) {
}

