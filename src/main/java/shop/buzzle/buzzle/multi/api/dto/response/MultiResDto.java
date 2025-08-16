package shop.buzzle.buzzle.multi.api.dto.response;

import lombok.Builder;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;

@Builder
public record MultiResDto(
        String roomId,
        String email,
        QuizResListDto quizzes
) {
}
