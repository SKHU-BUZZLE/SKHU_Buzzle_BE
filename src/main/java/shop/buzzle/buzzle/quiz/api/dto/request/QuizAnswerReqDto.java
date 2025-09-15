package shop.buzzle.buzzle.quiz.api.dto.request;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record QuizAnswerReqDto(
        String question,
        String option1,
        String option2,
        String option3,
        String option4,
        String correctAnswerNumber,
        String userAnswerNumber,
        QuizCategory category
) {
}