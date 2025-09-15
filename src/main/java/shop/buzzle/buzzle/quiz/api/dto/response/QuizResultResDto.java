package shop.buzzle.buzzle.quiz.api.dto.response;

import java.time.LocalDateTime;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

public record QuizResultResDto(
        Long id,
        String question,
        String option1,
        String option2,
        String option3,
        String option4,
        String correctAnswerNumber,
        String userAnswerNumber,
        QuizCategory category,
        Boolean isCorrect,
        LocalDateTime createdAt
) {

    public static QuizResultResDto from(QuizResult quizResult) {
        return new QuizResultResDto(
                quizResult.getId(),
                quizResult.getQuestion(),
                quizResult.getOption1(),
                quizResult.getOption2(),
                quizResult.getOption3(),
                quizResult.getOption4(),
                quizResult.getCorrectAnswerNumber(),
                quizResult.getUserAnswerNumber(),
                quizResult.getCategory(),
                quizResult.getIsCorrect(),
                quizResult.getCreatedAt()
        );
    }
}