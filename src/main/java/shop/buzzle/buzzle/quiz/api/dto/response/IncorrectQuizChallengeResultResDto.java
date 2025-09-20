package shop.buzzle.buzzle.quiz.api.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record IncorrectQuizChallengeResultResDto(
        int totalQuestions,
        int correctAnswers,
        int removedFromWrongNotes,
        List<QuizResultDto> results
) {
    
    @Builder
    public record QuizResultDto(
            Long quizResultId,
            String question,
            String userAnswer,
            String correctAnswer,
            boolean isCorrect,
            boolean removedFromWrongNotes
    ) {}
    
    public static IncorrectQuizChallengeResultResDto of(
            int totalQuestions,
            int correctAnswers,
            int removedFromWrongNotes,
            List<QuizResultDto> results
    ) {
        return IncorrectQuizChallengeResultResDto.builder()
                .totalQuestions(totalQuestions)
                .correctAnswers(correctAnswers)
                .removedFromWrongNotes(removedFromWrongNotes)
                .results(results)
                .build();
    }
}