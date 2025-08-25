package shop.buzzle.buzzle.game.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import shop.buzzle.buzzle.websocket.api.dto.Question;

@Getter
public class GameSession {
    private final List<Question> questions;
    private int currentQuestionIndex = 0;
    private boolean finished = false;
    private final Map<String, Integer> scores = new HashMap<>();

    private final AtomicBoolean correctAnswered = new AtomicBoolean(false); // ✅ 오직 1명만 정답 인정
    private final AtomicBoolean transitionLock = new AtomicBoolean(false);  // ✅ 문제 전환 중복 방지

    public GameSession(List<Question> questions) {
        this.questions = questions;
    }

    public Question getCurrentQuestion() {
        return questions.get(currentQuestionIndex);
    }

    public boolean tryAnswerCorrect(String username, int selectedIndex) {
        if (finished || correctAnswered.get()) return false;

        Question current = getCurrentQuestion();
        if (!current.isCorrectIndex(selectedIndex)) return false;

        // 오직 한 명만 정답자 인정
        if (correctAnswered.compareAndSet(false, true)) {
            addCorrectAnswer(username);
            return true;
        }
        return false;
    }

    public boolean tryNextQuestion() {
        if (transitionLock.compareAndSet(false, true)) {
            currentQuestionIndex++;
            correctAnswered.set(false);
            if (currentQuestionIndex >= questions.size()) {
                finished = true;
            }
            transitionLock.set(false);
            return true;
        }
        return false;
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
}
