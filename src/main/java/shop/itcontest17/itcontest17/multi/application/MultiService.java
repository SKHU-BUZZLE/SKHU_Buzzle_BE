package shop.itcontest17.itcontest17.multi.application;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.multi.api.dto.response.MultiResDto;
import shop.itcontest17.itcontest17.notification.application.NotificationService;
import shop.itcontest17.itcontest17.notification.application.SseEmitterManager;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResListDto;
import shop.itcontest17.itcontest17.quiz.application.QuizService;
import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiService {

    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;

    private final Queue<Member> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<MultiResDto>> waitingUsers = new HashMap<>();

    private final Lock lock = new ReentrantLock(); // 동시성 제어
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2); // 2개 스케줄러

    public CompletableFuture<MultiResDto> addToQueue(String email) {
        CompletableFuture<MultiResDto> future = new CompletableFuture<>();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        waitingUsers.put(member.getEmail(), future);
        waitingQueue.add(member);

        if (waitingQueue.size() >= 2) {
            Member user1 = waitingQueue.poll();
            Member user2 = waitingQueue.poll();

            String roomId = createRoomId();
            QuizSizeReqDto quizSizeReqDto = new QuizSizeReqDto(QuizCategory.ALL, 3);
            QuizResListDto quizList = quizService.askForAdvice(quizSizeReqDto);

            MultiResDto resultForUser1 = MultiResDto.builder()
                    .roomId(roomId)
                    .email(user2.getEmail())
                    .quizzes(quizList)
                    .build();

            MultiResDto resultForUser2 = MultiResDto.builder()
                    .roomId(roomId)
                    .email(user1.getEmail())
                    .quizzes(quizList)
                    .build();

            waitingUsers.get(user1.getEmail()).complete(resultForUser1);
            waitingUsers.get(user2.getEmail()).complete(resultForUser2);

            waitingUsers.remove(user1.getEmail());
            waitingUsers.remove(user2.getEmail());
        }

        return future;
    }

    private String createRoomId() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public boolean winnerProcessing(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        member.incrementStreak(QuizScore.MULTI_SCORE.getScore());
        return true;
    }

    @PostConstruct
    public void startSchedulers() {
        scheduler.scheduleAtFixedRate(this::matchUsers, 0, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::checkDisconnectedUsers, 5, 10, TimeUnit.SECONDS);
    }

    public String addToQueueV2(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        lock.lock();
        try {
            if (!waitingQueue.contains(member)) {
                waitingQueue.add(member);
                log.info(member.getEmail() + "님 매칭 대기 중");
                matchUsers();
                return "매칭 중";
            } else {
                return "이미 매칭 대기 중입니다.";
            }
        } finally {
            lock.unlock();
        }
    }

    private void matchUsers() {
        lock.lock();
        try {
            if (waitingQueue.size() >= 2) {
                Member user1 = waitingQueue.poll();
                Member user2 = waitingQueue.poll();

                if (user1 == null || user2 == null) {
                    retryMatch(user1);
                    retryMatch(user2);
                    return;
                }

                String roomId = createRoomId();
                String message = "roomId :" + roomId;

                notificationService.send(user1.getEmail(), message);
                notificationService.send(user2.getEmail(), message);

                log.info("✅ 매칭 완료: " + user1.getEmail() + " ↔ " + user2.getEmail());
            }
        } finally {
            lock.unlock();
        }
    }

    // ✅ SSE 연결 확인 (10초 간격 실행)
    private void checkDisconnectedUsers() {
        for (Member member : waitingQueue) {
            String email = member.getEmail();
            Long id = member.getId();

            SseEmitter emitter = sseEmitterManager.getEmitter(id);

            if (emitter == null) {
                removeDisconnectedUser(email);
                continue;
            }

            try {
                emitter.send(SseEmitter.event().name("ping").data("💓"));
            } catch (IOException e) {
                // 연결 끊김으로 간주하고 조용히 제거
                removeDisconnectedUser(email);
                log.info(email + " → SSE 연결 끊김 감지, 큐에서 제거됨");
            } catch (Exception ex) {
                // 다른 예외가 발생해도 로직 중단 방지
                log.info(email + " → SSE 상태 확인 중 알 수 없는 예외 발생: " + ex.getMessage());
            }
        }
    }

    private void removeDisconnectedUser(String email) {
        lock.lock();
        try {
            waitingQueue.removeIf(member -> member.getEmail().equals(email));
            log.info(email + " → 연결 끊김, 큐에서 제거됨");
        } finally {
            lock.unlock();
        }
    }

    private void retryMatch(Member user) {
        if (user != null) {
            lock.lock();
            try {
                waitingQueue.add(user);
            } finally {
                lock.unlock();
            }
        }
    }

    public Void cancelMatch(String email) {
        lock.lock();
        try {
            waitingQueue.removeIf(member -> member.getEmail().equals(email));
        } finally {
            lock.unlock();
        }
        return null;
    }
}
