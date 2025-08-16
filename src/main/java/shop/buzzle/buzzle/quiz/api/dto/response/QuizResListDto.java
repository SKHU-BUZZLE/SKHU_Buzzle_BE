package shop.buzzle.buzzle.quiz.api.dto.response;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (QuizResDto quiz : quizResDtos) {
            sb.append(quiz.toString()).append("\n"); // 각 퀴즈마다 줄 바꿈
        }
        return sb.toString();
    }
}
