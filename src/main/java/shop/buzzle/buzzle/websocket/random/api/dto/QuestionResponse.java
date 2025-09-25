package shop.buzzle.buzzle.websocket.random.api.dto;

import java.util.List;

public record QuestionResponse(
        String type,
        String question,
        List<String> options,
        int questionIndex
) {
    public static QuestionResponse of(String question, List<String> options, int questionIndex) {
        return new QuestionResponse("QUESTION", question, options, questionIndex);
    }
}
