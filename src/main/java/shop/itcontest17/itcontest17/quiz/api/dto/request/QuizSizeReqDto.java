package shop.itcontest17.itcontest17.quiz.api.dto.request;

import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;

public record QuizSizeReqDto(
        QuizCategory category,
        int size
) {
}

