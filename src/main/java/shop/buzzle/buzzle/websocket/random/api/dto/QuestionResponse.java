package shop.buzzle.buzzle.websocket.random.api.dto;

import shop.buzzle.buzzle.websocket.dto.MessageType;

import java.util.List;

public record QuestionResponse(
        MessageType type,
        String question,
        List<String> options,
        int questionIndex
) {
    public static QuestionResponse of(String question, List<String> options, int questionIndex) {
        return new QuestionResponse(MessageType.QUESTION, question, options, questionIndex);
    }
}
