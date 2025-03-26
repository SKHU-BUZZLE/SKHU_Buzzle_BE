package shop.itcontest17.itcontest17.multi.application;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.multi.api.dto.response.MultiResDto;
import shop.itcontest17.itcontest17.notification.application.NotificationService;
import shop.itcontest17.itcontest17.notification.domain.repository.NotificationRepository;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResListDto;
import shop.itcontest17.itcontest17.quiz.application.QuizService;
import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;

@Service
@RequiredArgsConstructor
public class MultiService {

    // 매칭이 성공하면 (큐를 매칭할 때마다 확인해서 이미 큐에 사람이 존재하면 서로 빼기)
    // 그러려면 채팅 넣으면 큐에 넣고 매칭 취소하면 큐에서 빼는 그런 구조가 필요할 듯?
    // 큐에 담은 정보를 토대로 2명이 매칭되면 그 사람들한테 초대 메세지 보내고
    // 그렇게 채팅방에 참여시키기 (방아이디 공통적으로 주고, 사용자 이름도 보내주면서)


    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final Queue<Member> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<MultiResDto>> waitingUsers = new HashMap<>();
    private final Lock lock = new ReentrantLock(); // 동시성 제어
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public CompletableFuture<MultiResDto> addToQueue(String email) {
        CompletableFuture<MultiResDto> future = new CompletableFuture<>();
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        // 사용자 대기열에 추가
        waitingUsers.put(member.getEmail(), future);
        waitingQueue.add(member);

        // 두 명이 모이면 매칭 실행
        if (waitingQueue.size() >= 2) {
            Member user1 = waitingQueue.poll();
            Member user2 = waitingQueue.poll();

            String roomId = createRoomId();

            QuizSizeReqDto quizSizeReqDto = new QuizSizeReqDto(QuizCategory.ALL, 3);
            QuizResListDto quizList = quizService.askForAdvice(user1.getEmail(), quizSizeReqDto);

            MultiResDto resultForUser1 = MultiResDto.builder()
                    .roomId(roomId)
                    .email(user2.getEmail())
                    .quizzes(quizList) // 퀴즈 포함
                    .build();

            MultiResDto resultForUser2 = MultiResDto.builder()
                    .roomId(roomId)
                    .email(user1.getEmail())
                    .quizzes(quizList) // 퀴즈 포함
                    .build();

            waitingUsers.get(user1.getEmail()).complete(resultForUser1);
            waitingUsers.get(user2.getEmail()).complete(resultForUser2);

            waitingUsers.remove(user1.getEmail());
            waitingUsers.remove(user2.getEmail());
        }

        return future;
    }


    private String createRoomId() {
        return UUID.randomUUID().toString(); // 고유한 roomId 생성
    }

    @Transactional
    public boolean winnerProcessing(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

        return true;
    }

    // 🔥 매칭 타이머 설정
    @PostConstruct
    public void startMatchingScheduler() {
        scheduler.scheduleAtFixedRate(this::matchUsers, 0, 1, TimeUnit.SECONDS);
    }

    public String addToQueueV2(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        lock.lock();
        try {
            if (!waitingQueue.contains(member)) {
                waitingQueue.add(member);
                System.out.println(member.getEmail() + "님 매칭 대기 중");
                matchUsers(); // 매칭 시도
                return "매칭 중";
            } else {
                return "이미 매칭 대기 중입니다.";
            }
        } finally {
            lock.unlock();
        }
    }

    // 🔥 매칭 로직 (타이머 기반 실행)
    private void matchUsers() {
        lock.lock(); // 동시성 문제 방지
        try {
            if (waitingQueue.size() >= 2) {
                Member user1 = waitingQueue.poll();
                Member user2 = waitingQueue.poll();

                if (user1 == null || user2 == null) {
                    // 매칭 실패 시 다시 큐에 추가
                    retryMatch(user1);
                    retryMatch(user2);
                    return;
                }

                String message = "roomId :" + createRoomId();

                notificationService.send(user1.getEmail(), message);
                notificationService.send(user2.getEmail(), message);

                System.out.println("✅ 매칭 완료: " + user1.getEmail() + " ↔ " + user2.getEmail());
            }
        } finally {
            lock.unlock();
        }
    }

    // 🔥 매칭 실패 시 다시 큐에 추가
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

    // 🔥 취소 처리 (큐에서 제거만 진행)
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