package shop.buzzle.buzzle.websocket.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import shop.buzzle.buzzle.quiz.application.QuizService;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.common.event.domain.*;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
import shop.buzzle.buzzle.websocket.common.messaging.WebSocketMessageDispatcher;
import shop.buzzle.buzzle.websocket.dto.AnswerResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 게임 이벤트 통합 테스트.
 *
 * 실제 Spring 컨텍스트에서 이벤트 발행 → 핸들러 → WebSocket 전송 흐름을 검증합니다.
 * WebSocketMessageDispatcher만 목킹하여 실제 WebSocket 연결 없이 테스트합니다.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.ai.openai.api-key=test-key"
})
@DisplayName("게임 이벤트 통합 테스트")
class GameEventIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private WebSocketMessageDispatcher dispatcher;

    @MockBean
    private QuizService quizService;

    private static final String ROOM_ID = "test-room-123";
    private static final String INVITE_CODE = "ABC123";
    private static final String PLAYER_EMAIL = "player@test.com";
    private static final String PLAYER_NAME = "TestPlayer";

    @Nested
    @DisplayName("이벤트 직접 발행 테스트 - 이벤트 → 핸들러 → dispatcher 흐름 검증")
    class DirectEventPublishTest {

        @Test
        @DisplayName("GameStartedEvent 발행 시 핸들러를 거쳐 dispatcher가 호출된다")
        void gameStartedEvent_shouldTriggerDispatcher() {
            // given
            GameStartedEvent event = new GameStartedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    List.of(PLAYER_EMAIL), 5, QuizCategory.ALL
            );

            // when - 이벤트 직접 발행
            eventPublisher.publishEvent(event);

            // then - dispatcher가 호출되었는지 검증
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("GAME_START");
            assertThat(payload.get("totalQuestions")).isEqualTo(5);
        }

        @Test
        @DisplayName("QuestionSentEvent 발행 시 핸들러를 거쳐 dispatcher가 호출된다")
        void questionSentEvent_shouldTriggerDispatcher() {
            // given
            QuestionSentEvent event = new QuestionSentEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "테스트 문제입니다",
                    List.of("A", "B", "C", "D"),
                    0
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("QUESTION");
            assertThat(payload.get("question")).isEqualTo("테스트 문제입니다");
            assertThat(payload.get("options")).isEqualTo(List.of("A", "B", "C", "D"));
        }

        @Test
        @DisplayName("AnswerValidatedEvent 발행 시 핸들러를 거쳐 dispatcher가 호출된다")
        void answerValidatedEvent_shouldTriggerDispatcher() {
            // given
            AnswerValidatedEvent event = new AnswerValidatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    PLAYER_EMAIL, PLAYER_NAME,
                    0, 0, 0, true, true
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<AnswerResponse> responseCaptor = ArgumentCaptor.forClass(AnswerResponse.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), responseCaptor.capture());

            AnswerResponse response = responseCaptor.getValue();
            assertThat(response.userEmail()).isEqualTo(PLAYER_EMAIL);
            assertThat(response.userName()).isEqualTo(PLAYER_NAME);
            assertThat(response.correct()).isTrue();
        }

        @Test
        @DisplayName("TimerTickEvent 발행 시 핸들러를 거쳐 dispatcher가 호출된다")
        void timerTickEvent_shouldTriggerDispatcher() {
            // given
            TimerTickEvent event = new TimerTickEvent(ROOM_ID, INVITE_CODE, GameType.INVITE, 5);

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("TIMER");
            assertThat(payload.get("remainingTime")).isEqualTo(5);
        }

        @Test
        @DisplayName("PlayerJoinedEvent 발행 시 핸들러를 거쳐 dispatcher가 호출된다")
        void playerJoinedEvent_shouldTriggerDispatcher() {
            // given
            PlayerJoinedEvent event = new PlayerJoinedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    PLAYER_EMAIL, PLAYER_NAME, "http://picture.url"
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("PLAYER_JOINED");
            assertThat(payload.get("email")).isEqualTo(PLAYER_EMAIL);
            assertThat(payload.get("name")).isEqualTo(PLAYER_NAME);
        }

        @Test
        @DisplayName("RANDOM 타입 이벤트는 INVITE 핸들러에서 무시되고 RANDOM 핸들러가 처리한다")
        void randomTypeEvent_shouldBeHandledByRandomHandler() {
            // given
            GameStartedEvent event = new GameStartedEvent(
                    ROOM_ID, INVITE_CODE, GameType.RANDOM,  // RANDOM 타입
                    List.of(PLAYER_EMAIL), 5, QuizCategory.ALL
            );

            // when
            eventPublisher.publishEvent(event);

            // then - INVITE 핸들러의 sendToRoom은 호출 안 됨
            verify(dispatcher, never()).sendToRoom(anyString(), any());
            // RANDOM 핸들러의 sendToGameRoom이 호출됨
            verify(dispatcher).sendToGameRoom(eq(ROOM_ID), any());
        }
    }

    @Nested
    @DisplayName("LeaderboardUpdatedEvent 테스트")
    class LeaderboardEventTest {

        @Test
        @DisplayName("리더보드 이벤트 발행 시 정확한 데이터가 전송된다")
        void leaderboardEvent_shouldSendCorrectData() {
            // given
            Map<String, Integer> scores = Map.of(
                    "player1@test.com", 3,
                    "player2@test.com", 1
            );
            Map<String, String> emailToName = Map.of(
                    "player1@test.com", "Player1",
                    "player2@test.com", "Player2"
            );

            LeaderboardUpdatedEvent event = new LeaderboardUpdatedEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    "player1@test.com", "Player1",
                    scores, emailToName
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("LEADERBOARD");
            assertThat(payload.get("currentLeader")).isEqualTo("Player1");
            assertThat(payload.get("scores")).isEqualTo(scores);
            assertThat(payload.get("emailToName")).isEqualTo(emailToName);
        }
    }

    @Nested
    @DisplayName("PlayerLeftEvent 테스트")
    class PlayerLeftEventTest {

        @Test
        @DisplayName("일반 플레이어 퇴장 시 PLAYER_LEFT 메시지가 전송된다")
        void regularPlayerLeft_shouldSendPlayerLeftMessage() {
            // given
            PlayerLeftEvent event = new PlayerLeftEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    PLAYER_EMAIL, PLAYER_NAME, false  // 방장 아님
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("PLAYER_LEFT");
        }

        @Test
        @DisplayName("방장 퇴장 시 ROOM_DISBANDED 메시지가 전송된다")
        void hostLeft_shouldSendRoomDisbandedMessage() {
            // given
            PlayerLeftEvent event = new PlayerLeftEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE,
                    PLAYER_EMAIL, PLAYER_NAME, true  // 방장
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("ROOM_DISBANDED");
        }
    }

    @Nested
    @DisplayName("TimerExpiredEvent 테스트")
    class TimerExpiredEventTest {

        @Test
        @DisplayName("타이머 만료 시 TIME_UP 메시지가 전송된다")
        void timerExpired_shouldSendTimeUpMessage() {
            // given
            TimerExpiredEvent event = new TimerExpiredEvent(
                    ROOM_ID, INVITE_CODE, GameType.INVITE, 0
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), payloadCaptor.capture());

            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload.get("type")).isEqualTo("TIME_UP");
            assertThat(payload.get("message")).isEqualTo("시간이 종료되었습니다!");
        }
    }

    @Nested
    @DisplayName("RoomNotificationEvent 테스트")
    class RoomNotificationEventTest {

        @Test
        @DisplayName("방 알림 이벤트 발행 시 payload가 그대로 전송된다")
        void roomNotification_shouldSendPayloadAsIs() {
            // given
            Map<String, Object> customPayload = Map.of(
                    "type", "LOADING",
                    "message", "3초 후 다음 문제가 전송됩니다."
            );

            RoomNotificationEvent event = new RoomNotificationEvent(
                    ROOM_ID, INVITE_CODE, customPayload, GameType.INVITE
            );

            // when
            eventPublisher.publishEvent(event);

            // then
            verify(dispatcher).sendToRoom(eq(INVITE_CODE), eq(customPayload));
        }
    }
}
