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

        Question current = session.getCurrentQuestion();
        boolean isCorrect = current.isCorrectIndex(index);

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketAnswerResponse.of(username, isCorrect, current.answerIndex())
        );

        if (isCorrect) {
            session.addCorrectAnswer(username);

            if (session.isLastQuestion()) {
                session.nextQuestion(); // 마지막 문제 이후로 이동
                handleGameEnd(roomId, session);
            } else {
                // 로딩 메시지 전송
                broadcastToRoom(roomId, "loading", "잠시 후 다음 문제가 전송됩니다.");

                // 3초 지연 후 다음 문제 전송
                CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() -> {
                    session.nextQuestion();
                    sendCurrentQuestion(roomId);
                });
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
