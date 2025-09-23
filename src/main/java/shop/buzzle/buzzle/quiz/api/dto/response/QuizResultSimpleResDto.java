package shop.buzzle.buzzle.quiz.api.dto.response;

import lombok.Builder;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

@Builder
public record QuizResultSimpleResDto(
        Long id,
        String question
) {
    public static QuizResultSimpleResDto from(QuizResult quizResult) {
        return QuizResultSimpleResDto.builder()
                .id(quizResult.getId())
                .question(quizResult.getQuestion())
                .build();
    }
}