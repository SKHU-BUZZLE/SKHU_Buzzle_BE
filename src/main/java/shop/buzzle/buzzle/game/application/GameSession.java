package shop.buzzle.buzzle.game.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import shop.buzzle.buzzle.websocket.api.dto.Question;

@Getter
public class GameSession {
    private final List<Question> questions;
    private int currentQuestionIndex = 0;
    private boolean finished = false;
    private final Map<String, Integer> scores = new HashMap<>();
    private final AtomicBoolean answered = new AtomicBoolean(false);

    public GameSession(List<Question> questions) {
        this.questions = questions;
    }

    public Question getCurrentQuestion() {
        return questions.get(currentQuestionIndex);
    }

    public void nextQuestion() {
        currentQuestionIndex++;
        answered.set(false); // 다음 문제로 넘어갈 때 초기화
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

    public boolean tryAnswering() {
        return answered.compareAndSet(false, true);
    }
}
