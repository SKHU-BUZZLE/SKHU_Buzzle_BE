package shop.buzzle.buzzle.quiz.domain;

import lombok.Getter;

@Getter
public enum QuizScore {
    PERSONAL_SCORE(10), MULTI_SCORE(100);

    private final int score;

    QuizScore(int score) {
        this.score = score;
    }
}
