package shop.buzzle.buzzle.multiroom.application;

import lombok.Getter;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.api.dto.Question;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class MultiRoomGameSession {
    private final String roomId;
    private final List<Question> questions;
    private final Set<String> playerEmails;
    private final QuizCategory category;

    private int currentQuestionIndex = 0;
    private boolean finished = false;
    private final Map<String, Integer> scores = new HashMap<>();

    private final AtomicBoolean correctAnswered = new AtomicBoolean(false);
    private final AtomicBoolean transitionLock = new AtomicBoolean(false);

    public MultiRoomGameSession(String roomId, List<Question> questions,
                               Set<String> playerEmails, QuizCategory category) {
        this.roomId = roomId;
        this.questions = questions;
        this.playerEmails = Set.copyOf(playerEmails);
        this.category = category;

        for (String email : playerEmails) {
            scores.put(email, 0);
        }
    }

    public Question getCurrentQuestion() {
        if (currentQuestionIndex >= questions.size()) {
            return null;
        }
        return questions.get(currentQuestionIndex);
    }

    public boolean tryAnswerCorrect(String playerEmail, int selectedIndex) {
        if (finished || correctAnswered.get()) return false;
        if (!playerEmails.contains(playerEmail)) return false;

        Question current = getCurrentQuestion();
        if (current == null || !current.isCorrectIndex(selectedIndex)) return false;

        if (correctAnswered.compareAndSet(false, true)) {
            addCorrectAnswer(playerEmail);
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

    public void addCorrectAnswer(String playerEmail) {
        scores.put(playerEmail, scores.getOrDefault(playerEmail, 0) + 1);
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

    public int getTotalQuestions() {
        return questions.size();
    }
}