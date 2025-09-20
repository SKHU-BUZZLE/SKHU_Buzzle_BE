package shop.buzzle.buzzle.quiz.api.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record IncorrectQuizChallengeResDto(
        List<ChallengeQuizDto> quizzes,
        int timeLimit
) {
    
    @Builder
    public record ChallengeQuizDto(
            Long quizResultId,
            String question,
            String option1,
            String option2,
            String option3,
            String option4
    ) {}
    
    public static IncorrectQuizChallengeResDto of(List<ChallengeQuizDto> quizzes, int timeLimit) {
        return IncorrectQuizChallengeResDto.builder()
                .quizzes(quizzes)
                .timeLimit(timeLimit)
                .build();
    }
}