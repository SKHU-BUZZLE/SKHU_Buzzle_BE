package shop.itcontest17.itcontest17.websocket.application;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.itcontest17.itcontest17.game.api.dto.WebSocketAnswerResponse;
import shop.itcontest17.itcontest17.game.api.dto.WebSocketQuestionResponse;
import shop.itcontest17.itcontest17.game.api.dto.WebSocketGameEndResponse;
import shop.itcontest17.itcontest17.game.application.GameSession;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResDto;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.application.QuizService;
import shop.itcontest17.itcontest17.quiz.domain.QuizCategory;
import shop.itcontest17.itcontest17.quiz.domain.QuizScore;
import shop.itcontest17.itcontest17.websocket.api.dto.Question;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            session.nextQuestion();

            if (session.isFinished()) {
                String winner = session.getWinner();

                Member member = memberRepository.findByEmail(winner)
                        .orElseThrow(MemberNotFoundException::new);

                member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

                messagingTemplate.convertAndSend(
                        "/topic/game/" + roomId,
                        WebSocketGameEndResponse.of(winner)
                );

                sessionMap.remove(roomId);
            } else {
                sendCurrentQuestion(roomId);
            }
        }
    }

    public void broadcastToRoom(String roomId, String type, String message) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                Map.of("type", type, "message", message)
        );
    }

    public void resendCurrentQuestionToUser(String roomId, String userEmail) {
        GameSession session = sessionMap.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();

        messagingTemplate.convertAndSend(
                "/topic/game/" + roomId,
                WebSocketQuestionResponse.of(q.text(), q.options())
        );
    }

}
