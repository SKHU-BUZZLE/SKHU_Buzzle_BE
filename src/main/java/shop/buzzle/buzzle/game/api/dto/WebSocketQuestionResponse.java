package shop.buzzle.buzzle.game.api.dto;

import java.util.List;

public record WebSocketQuestionResponse(
        String type,
        String question,
        List<String> options,
        int questionIndex
) {
    public static WebSocketQuestionResponse of(String question, List<String> options,int questionIndex) {
        return new WebSocketQuestionResponse("QUESTION", question, options, questionIndex);
    }
}
