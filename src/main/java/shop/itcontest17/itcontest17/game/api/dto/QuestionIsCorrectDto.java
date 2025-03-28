package shop.itcontest17.itcontest17.game.api.dto;

import java.util.List;

public record QuestionIsCorrectDto(
        String text, List<String> options, String answer
) {
    public boolean isCorrect(String submitted) {
        return answer.trim().equalsIgnoreCase(submitted.trim());
    }
}
