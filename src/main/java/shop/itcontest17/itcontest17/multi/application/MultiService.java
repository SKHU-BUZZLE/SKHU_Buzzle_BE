package shop.itcontest17.itcontest17.multi.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResListDto;
import shop.itcontest17.itcontest17.quiz.application.QuizService;
import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;

@Service
@RequiredArgsConstructor
public class MultiService {

    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final Queue<Member> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, CompletableFuture<MultiResDto>> waitingUsers = new HashMap<>();

    // 퀴즈 10개도 동시에 반환해주기. 기다리는 게 아니고 어떻게 처리할지 생각하기.
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
}