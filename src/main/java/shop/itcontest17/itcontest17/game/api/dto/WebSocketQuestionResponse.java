package shop.itcontest17.itcontest17.game.api.dto;

import java.util.List;

public record WebSocketQuestionResponse(
        String type, String question, List<String> options
) {
}
