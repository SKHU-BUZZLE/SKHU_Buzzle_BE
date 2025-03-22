package shop.itcontest17.itcontest17.quiz.api.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record QuizResListDto(
        List<QuizResDto> quizResDtos
) {
    public static QuizResListDto from(List<QuizResDto> quizResDtos) {
        return QuizResListDto.builder()
                .quizResDtos(quizResDtos)
                .build();
    }
}
