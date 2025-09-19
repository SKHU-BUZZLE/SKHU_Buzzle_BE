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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class WSRoomService {

    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final Map<String, GameSession> sessionMap = new ConcurrentHashMap<>();
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();
    private final Map<String, List<ScheduledFuture<?>>> roomTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

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

        // 타이머가 이미 실행 중이 아닌 경우에만 시작
        if (session.tryStartTimer()) {
            startQuestionTimer(roomId, 10);
        }
    }

    private void startQuestionTimer(String roomId, int seconds) {
        GameSession session = sessionMap.get(roomId);
        if (session == null) return;

        // 기존 타이머들 취소
        cancelRoomTimers(roomId);

        List<ScheduledFuture<?>> timerTasks = new ArrayList<>();

        // 타이머 카운트다운 스케줄
        for (int i = seconds; i > 0; i--) {
            final int currentSecond = i;
            ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
                // 세션이 끝났거나 타이머가 중단되었으면 타이머 중단
                if (session.isFinished() || !session.isTimerRunning()) return;

                Map<String, Object> timerPayload = Map.of(
                    "type", "TIMER",
                    "remainingTime", currentSecond
                );
                messagingTemplate.convertAndSend("/topic/game/" + roomId, timerPayload);
            }, seconds - i, TimeUnit.SECONDS);

            timerTasks.add(timerTask);
        }

        // 시간 종료 스케줄
        ScheduledFuture<?> timeUpTask = scheduler.schedule(() -> {
            // 세션이 끝났거나 타이머가 중단되었으면 시간 종료 처리하지 않음
            if (session.isFinished() || !session.isTimerRunning()) return;

            Map<String, Object> timeUpPayload = Map.of(
                "type", "TIME_UP",
                "message", "시간이 종료되었습니다!"
            );
            messagingTemplate.convertAndSend("/topic/game/" + roomId, timeUpPayload);

            // 시간 초과 처리
            if (!session.isFinished()) {
                roomLocks.putIfAbsent(roomId, new Object());
                synchronized (roomLocks.get(roomId)) {
                    // 마지막 문제인 경우 바로 게임 종료
                    if (session.getCurrentQuestionIndex() >= session.getTotalQuestions() - 1) {
                        session.tryNextQuestion(); // 게임을 finished 상태로 만들기
                        handleGameEnd(roomId, session);
                        roomLocks.remove(roomId);
                    } else {
                        // 마지막 문제가 아닌 경우 다음 문제로
                        if (session.tryNextQuestion()) {
                            if (session.isFinished()) {
                                handleGameEnd(roomId, session);
                                roomLocks.remove(roomId);
                            } else {
                                broadcastToRoom(roomId, "LOADING", "3초 후 다음 문제가 전송됩니다.");
                                scheduler.schedule(() -> {
                                    synchronized (roomLocks.get(roomId)) {
                                        GameSession currentSession = sessionMap.get(roomId);
                                        if (currentSession != null && !currentSession.isFinished()) {
                                            sendCurrentQuestion(roomId);
                                        }
                                    }
                                }, 3, TimeUnit.SECONDS);
                            }
                        }
                    }
                }
            }
        }, seconds, TimeUnit.SECONDS);

        timerTasks.add(timeUpTask);

        // 방별 타이머 저장
        roomTimers.put(roomId, timerTasks);
    }

    private void cancelRoomTimers(String roomId) {
        List<ScheduledFuture<?>> timers = roomTimers.remove(roomId);
        if (timers != null) {
            for (ScheduledFuture<?> timer : timers) {
                timer.cancel(false);
            }
        }
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

            int correctIndex = Integer.parseInt(current.answerIndex()) - 1;
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId,
                    WebSocketAnswerResponse.of(displayName, isCorrect, String.valueOf(correctIndex), String.valueOf(submittedIndex))
            );

            if (!isCorrect) return;

            boolean accepted = session.tryAnswerCorrect(email, submittedIndex);
            if (!accepted) return;

            // 정답 처리 후 현재 리더보드 정보 전송
            String currentLeader = session.getCurrentLeader();
            Member cyrrentLeaderMember = memberRepository.findByEmail(currentLeader)
                    .orElseThrow(MemberNotFoundException::new);
            Map<String, Integer> currentScores = session.getCurrentScores();
            messagingTemplate.convertAndSend(
                    "/topic/game/" + roomId,
                    LeaderboardResponse.of(cyrrentLeaderMember.getName(), currentScores)
            );

            if (session.tryNextQuestion()) {
                // 다음 문제로 넘어갈 때 현재 타이머 즉시 중단
                session.stopTimer();
                cancelRoomTimers(roomId);

                if (session.isFinished()) {
                    handleGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    // 타이머 중단 알림
                    Map<String, Object> timerStopPayload = Map.of(
                        "type", "TIMER_STOP",
                        "message", "정답! 다음 문제로 이동합니다."
                    );
                    messagingTemplate.convertAndSend("/topic/game/" + roomId, timerStopPayload);

                    broadcastToRoom(roomId, "LOADING", "3초 후 다음 문제가 전송됩니다.");
                    scheduler.schedule(() -> {
                        synchronized (roomLocks.get(roomId)) {
                            GameSession currentSession = sessionMap.get(roomId);
                            if (currentSession != null && !currentSession.isFinished()) {
                                sendCurrentQuestion(roomId);
                            }
                        }
                    }, 3, TimeUnit.SECONDS);
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
                WebSocketGameEndResponse.of(member.getName())
        );

        sessionMap.remove(roomId);
        cancelRoomTimers(roomId);
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
