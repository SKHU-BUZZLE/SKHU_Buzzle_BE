package shop.buzzle.buzzle.websocket.invite.game.application;

import lombok.Getter;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.dto.Question;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
public class InviteGameSession {
    private final String roomId;
    private final List<Question> questions;
    private final List<String> playerEmails;
    private final QuizCategory category;

    private int currentQuestionIndex = 0;
    private boolean finished = false;
    private final Map<String, Integer> scores = new HashMap<>();

    private final AtomicBoolean correctAnswered = new AtomicBoolean(false);
    private final AtomicBoolean transitionLock = new AtomicBoolean(false);
    private final AtomicBoolean timerRunning = new AtomicBoolean(false);

    public InviteGameSession(String roomId, List<Question> questions,
                             List<String> playerEmails, QuizCategory category) {
        this.roomId = roomId;
        this.questions = questions;
        this.playerEmails = List.copyOf(playerEmails);
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

    /**
     * 현재 문제의 정답 인덱스 반환 (0-based)
     * @return 정답 인덱스, 문제가 없으면 -1
     */
    public int getCorrectIndex() {
        Question current = getCurrentQuestion();
        if (current == null) return -1;
        return Integer.parseInt(current.answerIndex()) - 1;
    }

    /**
     * 답변 처리 - Value Object 반환 버전 (테스트 용이성 확보)
     * 기존 tryAnswerCorrect와 달리 side-effect 없이 결과만 반환합니다.
     *
     * @param playerEmail 플레이어 이메일
     * @param selectedIndex 선택한 답변 인덱스 (0-based)
     * @return 답변 처리 결과
     */
    public AnswerResult processAnswer(String playerEmail, int selectedIndex) {
        // 게임이 이미 종료됨
        if (finished) {
            return AnswerResult.rejected("게임이 이미 종료되었습니다.");
        }

        // 이미 이번 문제에 정답자가 있음
        if (correctAnswered.get()) {
            return AnswerResult.rejected("이미 이번 문제에 정답자가 있습니다.");
        }

        // 유효하지 않은 플레이어
        if (!playerEmails.contains(playerEmail)) {
            return AnswerResult.rejected("유효하지 않은 플레이어입니다.");
        }

        Question current = getCurrentQuestion();
        if (current == null) {
            return AnswerResult.rejected("현재 문제가 없습니다.");
        }

        int correctIndex = getCorrectIndex();
        boolean isCorrect = current.isCorrectIndex(selectedIndex);

        // 오답인 경우
        if (!isCorrect) {
            return AnswerResult.incorrect(playerEmail, selectedIndex, correctIndex);
        }

        // 정답인 경우 - 첫 번째 정답인지 확인 (CAS 연산)
        if (correctAnswered.compareAndSet(false, true)) {
            addCorrectAnswer(playerEmail);
            return AnswerResult.correct(playerEmail, selectedIndex, correctIndex, true);
        }

        // 정답이지만 이미 다른 사람이 먼저 정답을 제출함
        return AnswerResult.correct(playerEmail, selectedIndex, correctIndex, false);
    }
}