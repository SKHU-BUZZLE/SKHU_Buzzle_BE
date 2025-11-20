package shop.buzzle.buzzle.websocket.invite.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.websocket.invite.api.dto.InvitedRoom;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InvitedRoomCreateReqDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InvitedRoomJoinReqDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.invitedRoomCreateResDto;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class InviteRoomServiceConcurrencyTest {

    private InviteRoomService inviteRoomService;
    private MemberRepository memberRepository;
    private ApplicationEventPublisher eventPublisher;

    private String roomId;
    private String inviteCode;
    private final int MAX_PLAYERS = 2;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);

        inviteRoomService = new InviteRoomService(memberRepository, eventPublisher);

        Member host = Member.builder().email("host@test.com").name("Host").build();
        when(memberRepository.findByEmail("host@test.com")).thenReturn(Optional.of(host));

        InvitedRoomCreateReqDto createReqDto = new InvitedRoomCreateReqDto(MAX_PLAYERS, QuizCategory.ALL, 10);
        invitedRoomCreateResDto createResDto = inviteRoomService.createRoom("host@test.com", createReqDto);
        
        this.inviteCode = createResDto.inviteCode();
        this.roomId = inviteRoomService.validateInviteCode(this.inviteCode).roomInfo().roomId();

        InvitedRoomJoinReqDto joinReqDto = new InvitedRoomJoinReqDto(inviteCode);
        inviteRoomService.joinRoom("host@test.com", joinReqDto);
    }

    @Test
    @DisplayName("여러 사용자가 동시에 방 입장 시 정원을 초과하지 않는다")
    void joinRoom_concurrency_issue_pure_java_test() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        when(memberRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            if (email.equals("host@test.com")) {
                 return Optional.of(Member.builder().email(email).name("Host").build());
            }
            return Optional.of(Member.builder().email(email).name("Player").build());
        });

        // when
        for (int i = 0; i < numberOfThreads; i++) {
            final int userIndex = i;
            executorService.submit(() -> {
                try {
                    String playerEmail = "player" + userIndex + "@test.com";
                    InvitedRoomJoinReqDto joinReqDto = new InvitedRoomJoinReqDto(inviteCode);
                    inviteRoomService.joinRoom(playerEmail, joinReqDto);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // then
        InvitedRoom room = inviteRoomService.getRoom(this.roomId);
        int finalPlayerCount = room.getCurrentPlayerCount();

        System.out.println("==================================================");
        System.out.println("순수 Java 동시성 테스트 결과");
        System.out.println("방 최대 정원: " + room.getMaxPlayers());
        System.out.println("최종 입장 인원: " + finalPlayerCount);
        System.out.println("입장 성공 수 (방장 제외): " + successCount.get());
        System.out.println("입장 실패 수: " + failedCount.get());
        System.out.println("==================================================");

        // 동시성 제어가 정상적으로 동작하여, 방의 최대 인원을 초과하지 않아야 함 (테스트가 통과하면 정상)
        assertThat(finalPlayerCount).isEqualTo(room.getMaxPlayers());
    }
}
