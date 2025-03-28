package shop.itcontest17.itcontest17.game.application;

import java.util.List;
import lombok.Getter;
import shop.itcontest17.itcontest17.websocket.api.dto.Question;

@Getter
public class GameSession {
    private final List<Question> questions;
    private int currentQuestionIndex = 0;
    private boolean finished = false;

    public GameSession(List<Question> questions) {
        this.questions = questions;
    }

    public Question getCurrentQuestion() {
        return questions.get(currentQuestionIndex);
    }

    public void nextQuestion() {
        currentQuestionIndex++;
        if (currentQuestionIndex >= questions.size()) {
            finished = true;
        }
    }
}

