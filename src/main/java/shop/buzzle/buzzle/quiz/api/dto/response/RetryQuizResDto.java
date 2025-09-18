package shop.buzzle.buzzle.quiz.api.dto.response;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.quiz.domain.QuizResult;

public record RetryQuizResDto(
        Long originalQuizId,
        String question,
        String option1,
        String option2,
        String option3,
        String option4,
        String correctAnswer,
        QuizCategory category
) {

    public static RetryQuizResDto from(QuizResult quizResult) {
        return new RetryQuizResDto(
                quizResult.getId(),
                quizResult.getQuestion(),
                quizResult.getOption1(),
                quizResult.getOption2(),
                quizResult.getOption3(),
                quizResult.getOption4(),
                quizResult.getCorrectAnswerNumber(),
                quizResult.getCategory()
        );
    }
}