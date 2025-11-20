package shop.buzzle.buzzle.websocket.random.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.websocket.global.event.WSEventListener;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("랜덤 매칭 웹소켓 동시성 및 이벤트 테스트")
class RandomRoomConcurrencyTest {

    private WSEventListener wsEventListener;
    private RandomRoomService randomRoomService;
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        // Mock 객체 생성 (DB 접근 방지)
        randomRoomService = Mockito.mock(RandomRoomService.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        wsEventListener = new WSEventListener(randomRoomService, memberRepository);
    }

    @Test
    @DisplayName("하나의 방에 여러 사용자가 동시 참가 시 게임은 한 번만 시작되어야 한다")
    void whenMultipleUsersJoin_startGameShouldBeCalledOnlyOnce() throws InterruptedException {
        // given
        int numberOfThreads = 2;
        String roomId = "test-room-1";
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        when(memberRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return Optional.of(Member.builder().email(email).name("User-" + email).build());
        });

        // when
        // 2명의 사용자가 동시에 같은 방의 참가 이벤트를 발생시킴
        for (int i = 0; i < numberOfThreads; i++) {
            final String userEmail = "player" + i + "@test.com";
            executorService.submit(() -> {
                try {
                    wsEventListener.handleRegularRoomSubscribe(roomId, userEmail);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        ArgumentCaptor<List<String>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(randomRoomService, times(1)).startGame(eq(roomId), playersCaptor.capture());

        // 게임을 시작한 플레이어는 2명이어야 함
        List<String> startedPlayers = playersCaptor.getValue();
        assertThat(startedPlayers).hasSize(2);

        System.out.println("==================================================");
        System.out.println("하나의 방 동시 참가 테스트 결과");
        System.out.println("시도한 총 스레드 수: " + numberOfThreads);
        System.out.println("게임 시작 호출 횟수: 1");
        System.out.println("게임 시작 인원: " + startedPlayers.size());
        System.out.println("==================================================");
    }

    @Test
    @DisplayName("사용자 연결이 끊어지면 퇴장 처리가 정상적으로 수행되어야 한다")
    void whenUserDisconnects_shouldHandlePlayerLeft() {
        // given
        String roomId = "test-room-2";
        String userEmail1 = "player1@test.com";
        String userEmail2 = "player2@test.com";

        when(memberRepository.findByEmail(userEmail1)).thenReturn(Optional.of(Member.builder().email(userEmail1).name("Player1").build()));
        when(memberRepository.findByEmail(userEmail2)).thenReturn(Optional.of(Member.builder().email(userEmail2).name("Player2").build()));
        wsEventListener.handleRegularRoomSubscribe(roomId, userEmail1);
        wsEventListener.handleRegularRoomSubscribe(roomId, userEmail2);

        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create();
        headerAccessor.setSessionId("session-1");

        headerAccessor.setSessionAttributes(new ConcurrentHashMap<>());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        sessionAttributes.put("userEmail", userEmail1);
        sessionAttributes.put("roomId", roomId);
        sessionAttributes.put("destination", "/topic/game/" + roomId);

        Message<byte[]> message = new GenericMessage<>(new byte[0], headerAccessor.getMessageHeaders());

        SessionDisconnectEvent disconnectEvent = new SessionDisconnectEvent("test-source", message, "session-1", CloseStatus.NORMAL);

        // when
        wsEventListener.handleWebSocketDisconnectListener(disconnectEvent);

        // then
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(randomRoomService, times(1)).broadcastToRoom(eq(roomId), typeCaptor.capture(), messageCaptor.capture());

        assertThat(typeCaptor.getValue()).isEqualTo("PLAYER_LEFT");
        assertThat(messageCaptor.getValue()).contains(userEmail1);

        System.out.println("==================================================");
        System.out.println("연결 종료 이벤트 테스트 결과");
        System.out.println("발생 이벤트: " + userEmail1 + " 연결 종료");
        System.out.println("호출된 브로드캐스트 타입: " + typeCaptor.getValue());
        System.out.println("브로드캐스트 메시지: " + messageCaptor.getValue());
        System.out.println("==================================================");
    }

    @Test
    @DisplayName("여러 다른 방이 동시에 생성될 때 서로 영향을 주지 않아야 한다")
    void whenMultipleRoomsAreFormedConcurrently_shouldSucceedIndependently() throws InterruptedException {
        // given
        int numberOfThreads = 4;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        when(memberRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            return Optional.of(Member.builder().email(email).name("User-" + email).build());
        });

        // when
        // User 0, 1 -> room-A
        // User 2, 3 -> room-B
        for (int i = 0; i < numberOfThreads; i++) {
            final String userEmail = "concurrent-player" + i + "@test.com";
            final String roomId = (i < 2) ? "room-A" : "room-B";
            executorService.submit(() -> {
                try {
                    wsEventListener.handleRegularRoomSubscribe(roomId, userEmail);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        ArgumentCaptor<String> roomIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<String>> playersCaptor = ArgumentCaptor.forClass(List.class);
        verify(randomRoomService, times(2)).startGame(roomIdCaptor.capture(), playersCaptor.capture());

        List<String> capturedRoomIds = roomIdCaptor.getAllValues();
        List<List<String>> capturedPlayerLists = playersCaptor.getAllValues();

        assertThat(capturedRoomIds).containsExactlyInAnyOrder("room-A", "room-B");

        for (int i = 0; i < capturedRoomIds.size(); i++) {
            String roomId = capturedRoomIds.get(i);
            List<String> players = capturedPlayerLists.get(i);
            assertThat(players).hasSize(2);
            if (roomId.equals("room-A")) {
                assertThat(players).containsExactlyInAnyOrder("concurrent-player0@test.com", "concurrent-player1@test.com");
            } else if (roomId.equals("room-B")) {
                assertThat(players).containsExactlyInAnyOrder("concurrent-player2@test.com", "concurrent-player3@test.com");
            }
        }

        System.out.println("==================================================");
        System.out.println("여러 방 동시 생성 테스트 결과");
        System.out.println("시도한 총 스레드 수: " + numberOfThreads);
        System.out.println("총 게임 시작 호출 횟수: 2");
        System.out.println("==================================================");
    }
}
