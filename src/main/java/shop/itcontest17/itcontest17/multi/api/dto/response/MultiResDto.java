package shop.itcontest17.itcontest17.multi.api.dto.response;

import lombok.Builder;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResListDto;

@Builder
public record MultiResDto(
        String roomId,
        String email,
        QuizResListDto quizzes
) {
}
