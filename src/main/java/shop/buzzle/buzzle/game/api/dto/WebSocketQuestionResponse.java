package shop.buzzle.buzzle.game.api.dto;

import java.util.List;

public record WebSocketQuestionResponse(
        String type,
        String question,
        List<String> options
) {
    public static WebSocketQuestionResponse of(String question, List<String> options) {
        return new WebSocketQuestionResponse("QUESTION", question, options);
    }
}
