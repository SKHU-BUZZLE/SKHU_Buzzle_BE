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
import shop.buzzle.buzzle.websocket.api.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.api.dto.Question;
import shop.buzzle.buzzle.websocket.api.dto.LeaderboardResponse;
import shop.buzzle.buzzle.websocket.api.dto.PlayerJoinedResponse;

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
                WebSocketQuestionResponse.of(
                        q.text(),
                        q.options(),
                        session.getCurrentQuestionIndex()
                )
        );
    }


    @Transactional
    public void receiveAnswer(String roomId, String email, AnswerRequest answerRequest) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        int submittedIndex = answerRequest.index();
        int clientQuestionIndex = answerRequest.questionIndex();

        if (clientQuestionIndex != session.getCurrentQuestionIndex()) return;

        roomLocks.putIfAbsent(roomId, new Object());

        synchronized (roomLocks.get(roomId)) {
            Question current = session.getCurrentQuestion();
            boolean isCorrect = current.isCorrectIndex(submittedIndex);

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(MemberNotFoundException::new);
            String displayName = member.getName();

            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId,
                    WebSocketAnswerResponse.of(displayName, isCorrect, current.answerIndex())
            );

            if (!isCorrect) return;

            boolean accepted = session.tryAnswerCorrect(email, submittedIndex);
            if (!accepted) return;

            // 정답 처리 후 현재 리더보드 정보 전송
            String currentLeader = session.getCurrentLeader();
            Map<String, Integer> currentScores = session.getCurrentScores();
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId,
                    LeaderboardResponse.of(currentLeader, currentScores)
            );

            if (session.tryNextQuestion()) {
                if (session.isFinished()) {
                    handleGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    broadcastToRoom(roomId, "LOADING", "3초 후 다음 문제가 전송됩니다.");
                    CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
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
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("type", type);
        response.put("message", message);

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                response
        );
    }

    public void broadcastPlayerJoined(String roomId, PlayerJoinedResponse playerInfo) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                playerInfo
        );
    }

    public void resendCurrentQuestionToUser(String roomId) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketQuestionResponse.of(
                        q.text(),
                        q.options(),
                        session.getCurrentQuestionIndex()
                )
        );
    }

}
