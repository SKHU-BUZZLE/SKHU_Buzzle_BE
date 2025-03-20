package shop.itcontest17.itcontest17.quiz.api.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizDto {
    private String question;
    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String answer;
    private LocalDateTime nowTime;

    public static QuizDto of(String question, String option1, String option2, String option3, String option4, String answer) {
        return QuizDto.builder()
                .question(question)
                .option1(option1)
                .option2(option2)
                .option3(option3)
                .option4(option4)
                .answer(answer)
                .nowTime(LocalDateTime.now())
                .build();
    }
}