package shop.buzzle.buzzle.multi.application;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.multi.api.dto.response.MultiResDto;
import shop.buzzle.buzzle.multi.api.dto.response.MatchInfoDto;
import shop.buzzle.buzzle.notification.application.NotificationService;
import shop.buzzle.buzzle.notification.application.SseEmitterManager;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.application.QuizService;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.quiz.domain.QuizScore;

import java.io.IOException;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class MultiService {

    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final SseEmitterManager sseEmitterManager;

    private final Queue<Member> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> waitingEmails = ConcurrentHashMap.newKeySet(); // 이메일 기반 중복 방지

    private final Lock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

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
            if (!waitingEmails.contains(email)) {
                waitingEmails.add(email);
                waitingQueue.add(member);
                log.info("{}님 매칭 대기 중", email);
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
            while (waitingQueue.size() >= 2) {
                Member user1 = waitingQueue.poll();
                Member user2 = waitingQueue.poll();

                if (user1 == null || user2 == null) {
                    retryMatch(user1);
                    retryMatch(user2);
                    break;
                }

                // 같은 유저가 양쪽에 있을 경우 재시도
                if (user1.getEmail().equals(user2.getEmail())) {
                    log.warn("⚠️ 동일 유저 중복 매칭 시도: {}", user1.getEmail());
                    retryMatch(user1);
                    continue;
                }

                waitingEmails.remove(user1.getEmail());
                waitingEmails.remove(user2.getEmail());

                String roomId = createRoomId();

                // user1에게는 user2 정보를, user2에게는 user1 정보를 전송
                MatchInfoDto matchInfoForUser1 = MatchInfoDto.of(roomId, user2.getName(), user2.getPicture());
                MatchInfoDto matchInfoForUser2 = MatchInfoDto.of(roomId, user1.getName(), user1.getPicture());

                notificationService.send(user1.getEmail(), "matchInfo", matchInfoForUser1);
                notificationService.send(user2.getEmail(), "matchInfo", matchInfoForUser2);

                log.info("✅ 매칭 완료: {} ↔ {}", user1.getEmail(), user2.getEmail());
            }
        } finally {
            lock.unlock();
        }
    }

    private String createRoomId() {
        return UUID.randomUUID().toString();
    }

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
                removeDisconnectedUser(email);
                log.info("{} → SSE 연결 끊김 감지, 큐에서 제거됨", email);
            } catch (Exception ex) {
                log.info("{} → SSE 상태 확인 중 알 수 없는 예외 발생: {}", email, ex.getMessage());
            }
        }
    }

    private void removeDisconnectedUser(String email) {
        lock.lock();
        try {
            waitingQueue.removeIf(member -> member.getEmail().equals(email));
            waitingEmails.remove(email);
            log.info("{} → 연결 끊김, 큐에서 제거됨", email);
        } finally {
            lock.unlock();
        }
    }

    private void retryMatch(Member user) {
        if (user != null) {
            lock.lock();
            try {
                if (!waitingEmails.contains(user.getEmail())) {
                    waitingQueue.add(user);
                    waitingEmails.add(user.getEmail());
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public Void cancelMatch(String email) {
        lock.lock();
        try {
            waitingQueue.removeIf(member -> member.getEmail().equals(email));
            waitingEmails.remove(email);
            log.info("{} → 매칭 취소됨", email);
        } finally {
            lock.unlock();
        }
        return null;
    }
}
