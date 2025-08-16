package shop.buzzle.buzzle.quiz.api.dto.request;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record QuizReqDto(
        QuizCategory category
) {
}

