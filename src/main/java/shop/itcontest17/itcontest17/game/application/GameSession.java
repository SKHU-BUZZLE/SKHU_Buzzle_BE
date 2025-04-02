package shop.itcontest17.itcontest17.game.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import shop.itcontest17.itcontest17.websocket.api.dto.Question;

@Getter
public class GameSession {
    private final List<Question> questions;
    private int currentQuestionIndex = 0;
    private boolean finished = false;
    private final Map<String, Integer> scores = new HashMap<>();


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

    public void addCorrectAnswer(String username) {
        scores.put(username, scores.getOrDefault(username, 0) + 1);
    }

    public String getWinner() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean isLastQuestion() {
        return currentQuestionIndex >= questions.size() - 1;
    }
}

