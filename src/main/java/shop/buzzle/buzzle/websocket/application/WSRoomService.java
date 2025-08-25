package shop.buzzle.buzzle.websocket.application;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.game.api.dto.WebSocketAnswerResponse;
import shop.buzzle.buzzle.game.api.dto.WebSocketQuestionResponse;
import shop.buzzle.buzzle.game.api.dto.WebSocketGameEndResponse;
import shop.buzzle.buzzle.game.application.GameSession;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.application.QuizService;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import shop.buzzle.buzzle.quiz.domain.QuizScore;
import shop.buzzle.buzzle.websocket.api.dto.Question;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WSRoomService {

    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final Map<String, GameSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    public void startGame(String roomId) {
        List<QuizResDto> quizzes = quizService
                .askForAdvice(new QuizSizeReqDto(QuizCategory.ALL, 3))
                .quizResDtos();

        List<Question> questions = quizzes.stream()
                .map(q -> new Question(
                        q.question(),
                        List.of(q.option1(), q.option2(), q.option3(), q.option4()),
                        q.answer()
                ))
                .toList();

        GameSession session = new GameSession(questions);
        sessionMap.put(roomId, session);

        sendCurrentQuestion(roomId);
    }

    public void sendCurrentQuestion(String roomId) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketQuestionResponse.of(q.text(), q.options())
        );
    }

    @Transactional
    public void receiveAnswer(String roomId, String username, int index) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        roomLocks.putIfAbsent(roomId, new Object());

        synchronized (roomLocks.get(roomId)) {
            Question current = session.getCurrentQuestion();
            boolean isCorrect = current.isCorrectIndex(index);

            // 정답/오답 여부와 관계 없이 모두에게 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId,
                    WebSocketAnswerResponse.of(username, isCorrect, current.answerIndex())
            );

            // 정답이 아니면 아무것도 안 하고 종료 (계속 시도 가능하도록..)
            if (!isCorrect) return;

            // 정답일 경우: 오직 한 명만 정답자 인정하기. 동시성 잡는 로직 + 문제 넘기기
            boolean accepted = session.tryAnswerCorrect(username, index);
            if (!accepted) return;

            if (session.tryNextQuestion()) {
                if (session.isFinished()) {
                    handleGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    broadcastToRoom(roomId, "loading", "잠시 후 다음 문제가 전송됩니다.");

                    CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                        synchronized (roomLocks.get(roomId)) {
                            GameSession currentSession = sessionMap.get(roomId);
                            if (currentSession != null && !currentSession.isFinished()) {
                                sendCurrentQuestion(roomId);
                            }
                        }
                    });
                }
            }
        }
    }


    private void handleGameEnd(String roomId, GameSession session) {
        String winner = session.getWinner();

        Member member = memberRepository.findByEmail(winner)
                .orElseThrow(MemberNotFoundException::new);

        member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketGameEndResponse.of(winner)
        );

        sessionMap.remove(roomId);
    }

    public void broadcastToRoom(String roomId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                Map.of("type", type, "message", message)
        );
    }

    public void resendCurrentQuestionToUser(String roomId) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketQuestionResponse.of(q.text(), q.options())
        );
    }
}
