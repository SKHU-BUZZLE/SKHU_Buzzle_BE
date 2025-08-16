package shop.buzzle.buzzle.quiz.api.dto.response;

import java.time.LocalDateTime;

public record QuizResDto(
        String question,
        String option1,
        String option2,
        String option3,
        String option4,
        String answer,
        LocalDateTime nowTime
) {

    // 팩토리 메서드 필요 시 정의 가능
    public static QuizResDto of(String question, String option1, String option2, String option3, String option4, String answer) {
        return new QuizResDto(question, option1, option2, option3, option4, answer, LocalDateTime.now());
    }

    @Override
    public String toString() {
        return """
               Q: %s
               1. %s
               2. %s
               3. %s
               4. %s
               ✅ 정답: %s
               """.formatted(question, option1, option2, option3, option4, answer);
    }
}
