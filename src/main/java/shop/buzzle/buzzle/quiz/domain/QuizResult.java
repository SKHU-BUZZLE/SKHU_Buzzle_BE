package shop.buzzle.buzzle.quiz.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import shop.buzzle.buzzle.global.entity.BaseEntity;
import shop.buzzle.buzzle.member.domain.Member;

@Entity
@Getter
@NoArgsConstructor
public class QuizResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(columnDefinition = "TEXT")
    private String question;

    private String option1;
    private String option2;
    private String option3;
    private String option4;
    private String correctAnswerNumber;
    private String userAnswerNumber;

    @Enumerated(EnumType.STRING)
    private QuizCategory category;

    private Boolean isCorrect;

    @Builder
    private QuizResult(Member member, String question, String option1, String option2,
                      String option3, String option4, String correctAnswerNumber, String userAnswerNumber,
                      QuizCategory category, Boolean isCorrect) {
        this.member = member;
        this.question = question;
        this.option1 = option1;
        this.option2 = option2;
        this.option3 = option3;
        this.option4 = option4;
        this.correctAnswerNumber = correctAnswerNumber;
        this.userAnswerNumber = userAnswerNumber;
        this.category = category;
        this.isCorrect = isCorrect;
    }

    public static QuizResult createResult(Member member, String question, String option1, String option2,
                                        String option3, String option4, String correctAnswerNumber, String userAnswerNumber,
                                        QuizCategory category) {
        boolean isCorrect = correctAnswerNumber.equals(userAnswerNumber);

        return QuizResult.builder()
                .member(member)
                .question(question)
                .option1(option1)
                .option2(option2)
                .option3(option3)
                .option4(option4)
                .correctAnswerNumber(correctAnswerNumber)
                .userAnswerNumber(userAnswerNumber)
                .category(category)
                .isCorrect(isCorrect)
                .build();
    }
}