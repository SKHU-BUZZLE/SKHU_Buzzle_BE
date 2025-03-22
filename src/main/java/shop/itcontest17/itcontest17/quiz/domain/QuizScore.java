package shop.itcontest17.itcontest17.quiz.domain;

import lombok.Getter;

@Getter
public enum QuizScore {
    PERSONAL_SCORE(1), MULTI_SCORE(10);

    private final int score;

    QuizScore(int score) {
        this.score = score;
    }
}
