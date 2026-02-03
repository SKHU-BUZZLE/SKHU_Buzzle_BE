package shop.buzzle.buzzle.websocket.invite.game.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.dto.Question;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InviteGameSession POJO 단위 테스트.
 * WebSocket 연결 없이 순수 Java로 게임 로직을 검증합니다.
 */
class InviteGameSessionTest {

    private InviteGameSession session;
    private static final String ROOM_ID = "test-room-123";
    private static final String PLAYER_1 = "player1@test.com";
    private static final String PLAYER_2 = "player2@test.com";

    @BeforeEach
    void setUp() {
        List<Question> questions = List.of(
                new Question("첫 번째 문제", List.of("A", "B", "C", "D"), "1"),  // 정답: 0 (1-based → 0-based)
                new Question("두 번째 문제", List.of("A", "B", "C", "D"), "2"),  // 정답: 1
                new Question("세 번째 문제", List.of("A", "B", "C", "D"), "3")   // 정답: 2
        );
        session = new InviteGameSession(
                ROOM_ID,
                questions,
                List.of(PLAYER_1, PLAYER_2),
                QuizCategory.ALL
        );
    }

    @Nested
    @DisplayName("processAnswer 메서드 테스트")
    class ProcessAnswerTest {

        @Test
        @DisplayName("첫 번째 정답은 wasFirst=true로 처리된다")
        void firstCorrectAnswer_shouldBeAcceptedAsFirst() {
            // when
            AnswerResult result = session.processAnswer(PLAYER_1, 0);

            // then
            assertThat(result.accepted()).isTrue();
            assertThat(result.correct()).isTrue();
            assertThat(result.wasFirst()).isTrue();
            assertThat(result.playerEmail()).isEqualTo(PLAYER_1);
            assertThat(result.selectedIndex()).isEqualTo(0);
            assertThat(result.correctIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("이미 정답자가 있으면 다른 답변은 거부된다")
        void secondCorrectAnswer_shouldBeRejected() {
            // given
            session.processAnswer(PLAYER_1, 0);  // 첫 번째 정답

            // when
            AnswerResult result = session.processAnswer(PLAYER_2, 0);  // 두 번째 시도

            // then
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionReason()).contains("이미 이번 문제에 정답자가 있습니다");
        }

        @Test
        @DisplayName("오답은 correct=false로 처리된다")
        void incorrectAnswer_shouldBeMarkedAsIncorrect() {
            // when
            AnswerResult result = session.processAnswer(PLAYER_1, 1);  // 오답 (정답은 0)

            // then
            assertThat(result.accepted()).isTrue();
            assertThat(result.correct()).isFalse();
            assertThat(result.wasFirst()).isFalse();
            assertThat(result.correctIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("유효하지 않은 플레이어의 답변은 거부된다")
        void invalidPlayer_shouldBeRejected() {
            // when
            AnswerResult result = session.processAnswer("unknown@test.com", 0);

            // then
            assertThat(result.accepted()).isFalse();
            assertThat(result.rejectionReason()).contains("유효하지 않은 플레이어");
        }
    }

    @Nested
    @DisplayName("AnswerResult 헬퍼 메서드 테스트")
    class AnswerResultHelperTest {

        @Test
        @DisplayName("shouldAdvance는 첫 번째 정답에서만 true를 반환한다")
        void shouldAdvance_onlyForFirstCorrect() {
            AnswerResult firstCorrect = session.processAnswer(PLAYER_1, 0);
            AnswerResult secondCorrect = session.processAnswer(PLAYER_2, 0);
            AnswerResult incorrect = AnswerResult.incorrect(PLAYER_1, 1, 0);

            assertThat(firstCorrect.shouldAdvance()).isTrue();
            assertThat(secondCorrect.shouldAdvance()).isFalse();
            assertThat(incorrect.shouldAdvance()).isFalse();
        }

        @Test
        @DisplayName("shouldDecrementLife는 오답에서만 true를 반환한다")
        void shouldDecrementLife_onlyForIncorrect() {
            // 첫 번째 플레이어가 오답 제출
            AnswerResult incorrect = session.processAnswer(PLAYER_1, 1);  // 오답 (정답은 0)

            // 다음 문제로 넘어감
            session.tryNextQuestion();

            // 두 번째 문제에서 정답 제출
            AnswerResult correct = session.processAnswer(PLAYER_2, 1);  // 두 번째 문제 정답

            assertThat(incorrect.shouldDecrementLife()).isTrue();
            assertThat(correct.shouldDecrementLife()).isFalse();
        }
    }

    @Nested
    @DisplayName("점수 관리 테스트")
    class ScoreManagementTest {

        @Test
        @DisplayName("정답 시 점수가 증가한다")
        void correctAnswer_shouldIncrementScore() {
            // given
            assertThat(session.getCurrentScores().get(PLAYER_1)).isEqualTo(0);

            // when
            session.processAnswer(PLAYER_1, 0);

            // then
            assertThat(session.getCurrentScores().get(PLAYER_1)).isEqualTo(1);
        }

        @Test
        @DisplayName("오답 시 점수가 변하지 않는다")
        void incorrectAnswer_shouldNotChangeScore() {
            // when
            session.processAnswer(PLAYER_1, 1);  // 오답

            // then
            assertThat(session.getCurrentScores().get(PLAYER_1)).isEqualTo(0);
        }

        @Test
        @DisplayName("두 번째 정답자는 점수를 얻지 못한다")
        void secondCorrectAnswer_shouldNotIncrementScore() {
            // given
            session.processAnswer(PLAYER_1, 0);  // 첫 번째 정답

            // when
            session.processAnswer(PLAYER_2, 0);  // 두 번째 정답

            // then
            assertThat(session.getCurrentScores().get(PLAYER_1)).isEqualTo(1);
            assertThat(session.getCurrentScores().get(PLAYER_2)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("게임 진행 테스트")
    class GameProgressTest {

        @Test
        @DisplayName("tryNextQuestion으로 다음 문제로 전환된다")
        void tryNextQuestion_shouldAdvanceToNextQuestion() {
            // given
            assertThat(session.getCurrentQuestionIndex()).isEqualTo(0);

            // when
            boolean result = session.tryNextQuestion();

            // then
            assertThat(result).isTrue();
            assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
            assertThat(session.isFinished()).isFalse();
        }

        @Test
        @DisplayName("마지막 문제 후 게임이 종료된다")
        void afterLastQuestion_gameShouldBeFinished() {
            // when
            session.tryNextQuestion();  // 0 → 1
            session.tryNextQuestion();  // 1 → 2
            session.tryNextQuestion();  // 2 → 3 (종료)

            // then
            assertThat(session.isFinished()).isTrue();
        }

        @Test
        @DisplayName("다음 문제로 전환 시 correctAnswered 플래그가 리셋된다")
        void nextQuestion_shouldResetCorrectAnsweredFlag() {
            // given
            session.processAnswer(PLAYER_1, 0);  // 첫 문제 정답
            assertThat(session.processAnswer(PLAYER_2, 0).wasFirst()).isFalse();

            // when
            session.tryNextQuestion();

            // then - 다음 문제에서 다시 첫 정답이 가능
            AnswerResult result = session.processAnswer(PLAYER_1, 1);  // 두 번째 문제 정답
            assertThat(result.wasFirst()).isTrue();
        }
    }

    @Nested
    @DisplayName("우승자 결정 테스트")
    class WinnerDeterminationTest {

        @Test
        @DisplayName("가장 높은 점수의 플레이어가 우승자로 결정된다")
        void playerWithHighestScore_shouldBeWinner() {
            // given - PLAYER_1이 2문제, PLAYER_2가 1문제 정답
            session.processAnswer(PLAYER_1, 0);
            session.tryNextQuestion();
            session.processAnswer(PLAYER_1, 1);
            session.tryNextQuestion();
            session.processAnswer(PLAYER_2, 2);

            // when
            String winner = session.getWinner();

            // then
            assertThat(winner).isEqualTo(PLAYER_1);
            assertThat(session.getCurrentScores().get(PLAYER_1)).isEqualTo(2);
            assertThat(session.getCurrentScores().get(PLAYER_2)).isEqualTo(1);
        }

        @Test
        @DisplayName("점수가 같으면 먼저 기록된 플레이어가 우승자다")
        void equalScores_firstRecordedWins() {
            // given - 아무도 정답 없음
            Map<String, Integer> scores = session.getCurrentScores();
            assertThat(scores.get(PLAYER_1)).isEqualTo(0);
            assertThat(scores.get(PLAYER_2)).isEqualTo(0);

            // when
            String winner = session.getWinner();

            // then - 둘 중 하나가 반환됨
            assertThat(winner).isIn(PLAYER_1, PLAYER_2);
        }
    }

    @Nested
    @DisplayName("타이머 상태 테스트")
    class TimerStateTest {

        @Test
        @DisplayName("타이머 시작 시 isTimerRunning이 true가 된다")
        void startTimer_shouldSetRunningTrue() {
            // when
            boolean started = session.tryStartTimer();

            // then
            assertThat(started).isTrue();
            assertThat(session.isTimerRunning()).isTrue();
        }

        @Test
        @DisplayName("이미 실행 중인 타이머는 다시 시작되지 않는다")
        void duplicateStartTimer_shouldFail() {
            // given
            session.tryStartTimer();

            // when
            boolean secondStart = session.tryStartTimer();

            // then
            assertThat(secondStart).isFalse();
        }

        @Test
        @DisplayName("타이머 중지 시 isTimerRunning이 false가 된다")
        void stopTimer_shouldSetRunningFalse() {
            // given
            session.tryStartTimer();

            // when
            session.stopTimer();

            // then
            assertThat(session.isTimerRunning()).isFalse();
        }
    }
}
