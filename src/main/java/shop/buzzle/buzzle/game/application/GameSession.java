package shop.buzzle.buzzle.game.application;

import java.util.ArrayList;
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
    private final List<String> allPlayerEmails = new ArrayList<>();

    private final AtomicBoolean correctAnswered = new AtomicBoolean(false); 
    private final AtomicBoolean transitionLock = new AtomicBoolean(false);  
    private final AtomicBoolean timerRunning = new AtomicBoolean(false);    

    public GameSession(List<Question> questions) {
        this.questions = questions;
    }

    public GameSession(List<Question> questions, List<String> playerEmails) {
        this.questions = questions;
        this.allPlayerEmails.addAll(playerEmails);
        // 모든 플레이어를 0점으로 초기화
        for (String email : playerEmails) {
            scores.put(email, 0);
        }
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
            timerRunning.set(false);
            if (currentQuestionIndex >= questions.size()) {
                finished = true;
            }
            transitionLock.set(false);
            return true;
        }
        return false;
    }

    public boolean tryStartTimer() {
        return timerRunning.compareAndSet(false, true);
    }

    public void stopTimer() {
        timerRunning.set(false);
    }

    public boolean isTimerRunning() {
        return timerRunning.get();
    }

    public int getTotalQuestions() {
        return questions.size();
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

    public String getCurrentLeader() {
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public Map<String, Integer> getCurrentScores() {
        return new HashMap<>(scores);
    }

    public List<String> getAllPlayerEmails() {
        return allPlayerEmails.isEmpty() ? scores.keySet().stream().toList() : new ArrayList<>(allPlayerEmails);
    }
}
