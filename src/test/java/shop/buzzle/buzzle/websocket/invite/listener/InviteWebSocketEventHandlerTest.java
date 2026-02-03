package shop.buzzle.buzzle.websocket.invite.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.common.event.domain.*;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
import shop.buzzle.buzzle.websocket.common.messaging.WebSocketMessageDispatcher;
import shop.buzzle.buzzle.websocket.dto.AnswerResponse;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.GameEndResDto;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * InviteWebSocketEventHandler 단위 테스트.
 * 이벤트가 올바르게 WebSocket 메시지로 변환되어 전송되는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class InviteWebSocketEventHandlerTest {

    @Mock
    private WebSocketMessageDispatcher dispatcher;

    @InjectMocks
    private InviteWebSocketEventHandler handler;

    private static final String ROOM_ID = "test-room-123";
    private static final String INVITE_CODE = "ABC123";

    @Nested
    @DisplayName("TimerTickEvent 처리")
    class TimerTickEventTest {

        @Test
        @DisplayName("INVITE 타입 이벤트는 WebSocket 메시지로 전송된다")
        void inviteEvent_shouldSendMessage() {
            // given
            TimerTickEvent event = new TimerTickEvent(ROOM_ID, INVITE_CODE, GameType.INVITE, 5);

            // when
            handler.onTimerTick(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("TIMER");
            assertThat(payload.get("remainingTime")).isEqualTo(5);
        }

        @Test
        @DisplayName("RANDOM 타입 이벤트는 무시된다")
        void randomEvent_shouldBeIgnored() {
            // given
            TimerTickEvent event = new TimerTickEvent(ROOM_ID, INVITE_CODE, GameType.RANDOM, 5);

            // when
            handler.onTimerTick(event);

            // then
            verifyNoInteractions(dispatcher);
        }
    }

    @Nested
    @DisplayName("TimerExpiredEvent 처리")
    class TimerExpiredEventTest {

        @Test
        @DisplayName("타이머 만료 시 TIME_UP 메시지가 전송된다")
        void timerExpired_shouldSendTimeUpMessage() {
            // given
            TimerExpiredEvent event = new TimerExpiredEvent(ROOM_ID, INVITE_CODE, GameType.INVITE, 0);

            // when
            handler.onTimerExpired(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("TIME_UP");
            assertThat(payload.get("message")).isEqualTo("시간이 종료되었습니다!");
        }
    }

    @Nested
    @DisplayName("GameStartedEvent 처리")
    class GameStartedEventTest {

        @Test
        @DisplayName("게임 시작 시 GAME_START 메시지가 전송된다")
        void gameStarted_shouldSendGameStartMessage() {
            // given
            GameStartedEvent event = new GameStartedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    List.of("player1@test.com", "player2@test.com"),
                    5, QuizCategory.ALL
            );

            // when
            handler.onGameStarted(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("GAME_START");
            assertThat(payload.get("totalQuestions")).isEqualTo(5);
            assertThat(payload.get("countdownSeconds")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("QuestionSentEvent 처리")
    class QuestionSentEventTest {

        @Test
        @DisplayName("문제 전송 시 QUESTION 메시지가 전송된다")
        void questionSent_shouldSendQuestionMessage() {
            // given
            QuestionSentEvent event = new QuestionSentEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "테스트 문제입니다",
                    List.of("A", "B", "C", "D"),
                    0
            );

            // when
            handler.onQuestionSent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("QUESTION");
            assertThat(payload.get("question")).isEqualTo("테스트 문제입니다");
            assertThat(payload.get("options")).isEqualTo(List.of("A", "B", "C", "D"));
            assertThat(payload.get("questionIndex")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("AnswerValidatedEvent 처리")
    class AnswerValidatedEventTest {

        @Test
        @DisplayName("답변 검증 결과가 AnswerResponse로 전송된다")
        void answerValidated_shouldSendAnswerResponse() {
            // given
            AnswerValidatedEvent event = new AnswerValidatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "player1@test.com", "Player1",
                    0, 1, 0, false, false  // 오답
            );

            // when
            handler.onAnswerValidated(event);

            // then
            ArgumentCaptor<AnswerResponse> responseCaptor = ArgumentCaptor.forClass(AnswerResponse.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), responseCaptor.capture());

            AnswerResponse response = responseCaptor.getValue();
            assertThat(response.userEmail()).isEqualTo("player1@test.com");
            assertThat(response.userName()).isEqualTo("Player1");
            assertThat(response.correct()).isFalse();
        }
    }

    @Nested
    @DisplayName("LeaderboardUpdatedEvent 처리")
    class LeaderboardUpdatedEventTest {

        @Test
        @DisplayName("리더보드 갱신 시 LEADERBOARD 메시지가 전송된다")
        void leaderboardUpdated_shouldSendLeaderboardMessage() {
            // given
            Map<String, Integer> scores = Map.of("player1@test.com", 2, "player2@test.com", 1);
            Map<String, String> emailToName = Map.of("player1@test.com", "Player1", "player2@test.com", "Player2");

            LeaderboardUpdatedEvent event = new LeaderboardUpdatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "player1@test.com", "Player1",
                    scores, emailToName
            );

            // when
            handler.onLeaderboardUpdated(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("LEADERBOARD");
            assertThat(payload.get("currentLeader")).isEqualTo("Player1");
            assertThat(payload.get("currentLeaderEmail")).isEqualTo("player1@test.com");
            assertThat(payload.get("scores")).isEqualTo(scores);
            assertThat(payload.get("emailToName")).isEqualTo(emailToName);
        }

        @Test
        @DisplayName("리더가 null일 때 빈 문자열로 전송된다")
        void nullLeader_shouldSendEmptyString() {
            // given
            LeaderboardUpdatedEvent event = new LeaderboardUpdatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    null, null,
                    Map.of(), Map.of()
            );

            // when
            handler.onLeaderboardUpdated(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("currentLeader")).isEqualTo("");
            assertThat(payload.get("currentLeaderEmail")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("PlayerJoinedEvent 처리")
    class PlayerJoinedEventTest {

        @Test
        @DisplayName("플레이어 입장 시 PLAYER_JOINED 메시지가 전송된다")
        void playerJoined_shouldSendPlayerJoinedMessage() {
            // given
            PlayerJoinedEvent event = new PlayerJoinedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "player1@test.com", "Player1", "http://picture.url"
            );

            // when
            handler.onPlayerJoined(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("PLAYER_JOINED");
            assertThat(payload.get("email")).isEqualTo("player1@test.com");
            assertThat(payload.get("name")).isEqualTo("Player1");
            assertThat(payload.get("picture")).isEqualTo("http://picture.url");
        }
    }

    @Nested
    @DisplayName("PlayerLeftEvent 처리")
    class PlayerLeftEventTest {

        @Test
        @DisplayName("일반 플레이어 퇴장 시 PLAYER_LEFT 메시지가 전송된다")
        void regularPlayerLeft_shouldSendPlayerLeftMessage() {
            // given
            PlayerLeftEvent event = new PlayerLeftEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "player2@test.com", "Player2", false
            );

            // when
            handler.onPlayerLeft(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("PLAYER_LEFT");
            assertThat(payload.get("email")).isEqualTo("player2@test.com");
            assertThat(payload.get("name")).isEqualTo("Player2");
        }

        @Test
        @DisplayName("방장 퇴장 시 ROOM_DISBANDED 메시지가 전송된다")
        void hostLeft_shouldSendRoomDisbandedMessage() {
            // given
            PlayerLeftEvent event = new PlayerLeftEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "host@test.com", "Host", true
            );

            // when
            handler.onPlayerLeft(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("ROOM_DISBANDED");
            assertThat((String) payload.get("message")).contains("방장이 퇴장");
        }
    }

    @Nested
    @DisplayName("GameType 필터링")
    class GameTypeFilteringTest {

        @Test
        @DisplayName("모든 이벤트는 RANDOM 타입일 때 무시된다")
        void allEvents_shouldIgnoreRandomType() {
            // given
            GameStartedEvent gameStarted = new GameStartedEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    List.of("player@test.com"), 5, QuizCategory.ALL
            );
            QuestionSentEvent questionSent = new QuestionSentEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    "Question", List.of("A", "B", "C", "D"), 0
            );
            AnswerValidatedEvent answerValidated = new AnswerValidatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    "player@test.com", "Player", 0, 0, 0, true, true
            );
            LeaderboardUpdatedEvent leaderboardUpdated = new LeaderboardUpdatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    "player@test.com", "Player", Map.of(), Map.of()
            );
            PlayerJoinedEvent playerJoined = new PlayerJoinedEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    "player@test.com", "Player", null
            );
            PlayerLeftEvent playerLeft = new PlayerLeftEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,
                    "player@test.com", "Player", false
            );

            // when
            handler.onGameStarted(gameStarted);
            handler.onQuestionSent(questionSent);
            handler.onAnswerValidated(answerValidated);
            handler.onLeaderboardUpdated(leaderboardUpdated);
            handler.onPlayerJoined(playerJoined);
            handler.onPlayerLeft(playerLeft);

            // then
            verifyNoInteractions(dispatcher);
        }
    }
}
