package shop.buzzle.buzzle.multiroom.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomJoinReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomEventResponse;
import shop.buzzle.buzzle.multiroom.domain.MultiRoom;
import shop.buzzle.buzzle.multiroom.event.MultiRoomGameStartEvent;
import shop.buzzle.buzzle.multiroom.exception.MultiRoomNotFoundException;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.application.QuizService;
import shop.buzzle.buzzle.quiz.domain.QuizScore;
import shop.buzzle.buzzle.websocket.api.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.api.dto.Question;
import shop.buzzle.buzzle.game.api.dto.WebSocketAnswerResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiRoomWebSocketService {

    private final MultiRoomService multiRoomService;
    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    private final Map<String, MultiRoomGameSession> gameSessions = new ConcurrentHashMap<>();
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();

    public void joinRoom(String playerEmail, MultiRoomJoinReqDto request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 널 값 검증
            if (playerEmail == null || playerEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("플레이어 이메일이 비어있습니다.");
            }

            if (request == null) {
                throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
            }

            log.info("🚪 [ROOM_JOIN_START] Player: {}, InviteCode: {}", playerEmail, request.inviteCode());

            var roomInfo = multiRoomService.joinRoom(playerEmail, request);

            if (roomInfo == null) {
                throw new RuntimeException("방 참가 응답이 null입니다.");
            }

            String roomId = roomInfo.roomId();
            String inviteCode = roomInfo.inviteCode();

            if (roomId == null || inviteCode == null) {
                log.error("❌ [ROOM_JOIN_ERROR] roomId or inviteCode is null. roomId: {}, inviteCode: {}", roomId, inviteCode);
                throw new RuntimeException("방 ID 또는 초대코드가 null입니다.");
            }

            if (headerAccessor != null && headerAccessor.getSessionAttributes() != null) {
                headerAccessor.getSessionAttributes().put("roomId", roomId);
                headerAccessor.getSessionAttributes().put("inviteCode", inviteCode);
            } else {
                log.warn("⚠️ [ROOM_JOIN_WARNING] headerAccessor or sessionAttributes is null");
            }

            messagingTemplate.convertAndSendToUser(
                    playerEmail,
                    "/queue/room",
                    MultiRoomEventResponse.joinedRoom(roomInfo)
            );

            MultiRoom room = multiRoomService.getRoom(roomId);
            if (room == null) {
                log.error("❌ [ROOM_JOIN_ERROR] room is null after join. roomId: {}", roomId);
                throw new RuntimeException("방 참가 후 방을 찾을 수 없습니다.");
            }

            Member player = memberRepository.findByEmail(playerEmail)
                    .orElseThrow(() -> new MemberNotFoundException("참가자를 찾을 수 없습니다."));

            // 방 전체에 입장 알림
            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    MultiRoomEventResponse.playerJoined(player)
            );

        } catch (Exception e) {
            log.error("❌ [ROOM_JOIN_ERROR] Player: {}, Error: {}", playerEmail, e.getMessage(), e);

            if (playerEmail != null && messagingTemplate != null) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            playerEmail,
                            "/queue/room",
                            MultiRoomEventResponse.error("방 참가 실패: " + e.getMessage())
                    );
                } catch (Exception sendError) {
                    log.error("❌ [ERROR_SEND_FAILED] Failed to send error message to user: {}", sendError.getMessage());
                }
            }
        }
    }

    public void leaveRoom(String roomId, String playerEmail) {
        try {
            MultiRoom room = multiRoomService.getRoom(roomId);
            if (room == null) return;

            String inviteCode = room.getInviteCode();
            boolean isHost = room.isHost(playerEmail);

            multiRoomService.leaveRoom(roomId, playerEmail);

            // 방장이 나가면 방 폭파, 아니면 퇴장 알림
            if (isHost) {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + inviteCode,
                        MultiRoomEventResponse.message("방장이 퇴장하여 방이 해체되었습니다.")
                );
                gameSessions.remove(roomId);
                roomLocks.remove(roomId);
                log.info("❌ [ROOM_DISBANDED] Host left, InviteCode: {} disbanded", inviteCode);
            } else {
                Member player = memberRepository.findByEmail(playerEmail)
                        .orElse(null);
                String playerName = player != null ? player.getName() : playerEmail;

                messagingTemplate.convertAndSend(
                        "/topic/room/" + inviteCode,
                        MultiRoomEventResponse.playerLeft(playerName)
                );
                log.info("✅ [PLAYER_LEFT] Player: {} ({}), InviteCode: {}", playerName, playerEmail, inviteCode);
            }
        } catch (Exception e) {
            log.error("❌ [LEAVE_ROOM_ERROR] Player: {}, Error: {}", playerEmail, e.getMessage());
        }
    }

    public void startGame(String roomId, String hostEmail) {
        try {
            MultiRoom room = multiRoomService.getRoom(roomId);
            if (room == null) {
                throw new MultiRoomNotFoundException();
            }

            String inviteCode = room.getInviteCode();

            log.info("✅ [GAME_START_REQUEST] Host: {}, Room: {}, Players: {}/{}",
                    hostEmail, inviteCode, room.getCurrentPlayerCount(), room.getMaxPlayers());

            multiRoomService.startGame(roomId, hostEmail);

            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    MultiRoomEventResponse.message("게임이 시작됩니다!")
            );

        } catch (Exception e) {
            MultiRoom room = multiRoomService.getRoom(roomId);
            String inviteCode = room != null ? room.getInviteCode() : "unknown";

            log.error("❌ [GAME_START_ERROR] Room: {}, Error: {}", inviteCode, e.getMessage());

            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    MultiRoomEventResponse.error("게임 시작 실패: " + e.getMessage())
            );
        }
    }

    @Transactional
    public void startMultiRoomGame(String roomId) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) throw new MultiRoomNotFoundException();

        String inviteCode = room.getInviteCode();

        log.info("✅ [GAME_STARTING] Room: {}, Players: {}, Category: {}, Quiz Count: {}",
                inviteCode, room.getCurrentPlayerCount(), room.getCategory(), room.getQuizCount());

        List<QuizResDto> quizzes = quizService
                .askForAdvice(new QuizSizeReqDto(room.getCategory(), room.getQuizCount()))
                .quizResDtos();

        List<Question> questions = quizzes.stream()
                .map(q -> new Question(
                        q.question(),
                        List.of(q.option1(), q.option2(), q.option3(), q.option4()),
                        q.answer()
                ))
                .toList();

        MultiRoomGameSession session = new MultiRoomGameSession(
                roomId,
                questions,
                room.getPlayerEmails(),
                room.getCategory()
        );

        gameSessions.put(roomId, session);

        Map<String, Object> gameStartPayload = Map.of(
            "type", "GAME_START",
            "totalQuestions", session.getTotalQuestions(),
            "countdownSeconds", 3
        );
        messagingTemplate.convertAndSend("/topic/room/" + inviteCode, gameStartPayload);

        log.info("✅ [GAME_COUNTDOWN] Room: {}, Starting in 3 seconds...", inviteCode);

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
            sendCurrentQuestion(roomId);
        });
    }

    public void sendCurrentQuestion(String roomId) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) return;
        String inviteCode = room.getInviteCode();

        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();
        if (q == null) return;

        Map<String, Object> payload = Map.of(
            "type", "QUESTION",
            "question", q.text(),
            "options", q.options(),
            "questionIndex", session.getCurrentQuestionIndex()
        );

        messagingTemplate.convertAndSend("/topic/room/" + inviteCode, payload);

        // 10초 타이머 시작
        startQuestionTimer(roomId, inviteCode, 10);
    }

    private void startQuestionTimer(String roomId, String inviteCode, int seconds) {
        for (int i = seconds; i > 0; i--) {
            final int currentSecond = i;
            CompletableFuture.delayedExecutor(seconds - i, TimeUnit.SECONDS).execute(() -> {
                Map<String, Object> timerPayload = Map.of(
                    "type", "TIMER",
                    "remainingTime", currentSecond
                );
                messagingTemplate.convertAndSend("/topic/room/" + inviteCode, timerPayload);
            });
        }

        // 10초 후 시간 종료 메시지
        CompletableFuture.delayedExecutor(seconds, TimeUnit.SECONDS).execute(() -> {
            Map<String, Object> timeUpPayload = Map.of(
                "type", "TIME_UP",
                "message", "시간이 종료되었습니다!"
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, timeUpPayload);
        });
    }

    @Transactional
    public void receiveMultiRoomAnswer(String roomId, String email, AnswerRequest answerRequest) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();
        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        if (answerRequest.questionIndex() != session.getCurrentQuestionIndex()) return;

        roomLocks.putIfAbsent(roomId, new Object());

        synchronized (roomLocks.get(roomId)) {
            Question current = session.getCurrentQuestion();
            if (current == null) return;

            boolean isCorrect = current.isCorrectIndex(answerRequest.index());

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(MemberNotFoundException::new);
            String displayName = member.getName();

            int correctIndex = Integer.parseInt(current.answerIndex()) - 1;

            log.info("📝 [ANSWER_RECEIVED] Player: {}, Room: {}, Question: {}, Answer: {}, Correct: {}",
                    displayName, inviteCode, answerRequest.questionIndex() + 1, answerRequest.index() + 1, isCorrect);

            // ANSWER_RESULT 이벤트 전송
            WebSocketAnswerResponse answerResponse = WebSocketAnswerResponse.of(
                displayName,
                isCorrect,
                String.valueOf(correctIndex),
                String.valueOf(answerRequest.index())
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, answerResponse);

            if (!isCorrect) return;

            boolean accepted = session.tryAnswerCorrect(email, answerRequest.index());
            if (!accepted) {
                log.warn("⚠️ [DUPLICATE_ANSWER] Player: {} already answered correctly for this question", displayName);
                return;
            }

            // LEADERBOARD 이벤트 전송
            String currentLeaderEmail = session.getCurrentLeader();
            String currentLeaderName = currentLeaderEmail != null ?
                memberRepository.findByEmail(currentLeaderEmail)
                    .map(Member::getName)
                    .orElse(currentLeaderEmail) : null;

            // 점수를 이름으로 변환
            Map<String, Integer> nameScores = new HashMap<>();
            for (Map.Entry<String, Integer> entry : session.getCurrentScores().entrySet()) {
                String name = memberRepository.findByEmail(entry.getKey())
                    .map(Member::getName)
                    .orElse(entry.getKey());
                nameScores.put(name, entry.getValue());
            }

            Map<String, Object> leaderboardPayload = Map.of(
                "type", "LEADERBOARD",
                "currentLeader", currentLeaderName,
                "scores", nameScores
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, leaderboardPayload);

            if (session.tryNextQuestion()) {
                if (session.isFinished()) {
                    log.info("🏁 [GAME_FINISHED] Room: {}, Moving to game end", inviteCode);
                    handleMultiRoomGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    log.info("⏭️ [NEXT_QUESTION] Room: {}, Question {}/{} completed, preparing next question",
                            inviteCode, session.getCurrentQuestionIndex(), session.getTotalQuestions());
                    Map<String, Object> loadingPayload = Map.of(
                        "type", "LOADING",
                        "message", "3초 후 다음 문제가 전송됩니다."
                    );
                    messagingTemplate.convertAndSend("/topic/room/" + inviteCode, loadingPayload);
                    CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                        synchronized (roomLocks.get(roomId)) {
                            sendCurrentQuestion(roomId);
                        }
                    });
                }
            }
        }
    }

    private void handleMultiRoomGameEnd(String roomId, MultiRoomGameSession session) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();
        String winner = session.getWinner();

        if (winner != null) {
            Member member = memberRepository.findByEmail(winner)
                    .orElseThrow(MemberNotFoundException::new);

            member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

            log.info("🏆 [GAME_WINNER] Room: {}, Winner: {}", inviteCode, member.getName());

            Map<String, Object> gameEndPayload = Map.of(
                "type", "GAME_END",
                "message", "게임이 종료되었습니다! 우승자: " + member.getName(),
                "winner", member.getName()
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, gameEndPayload);
        } else {
            log.info("🤝 [GAME_TIE] Room: {}, No clear winner", inviteCode);
        }

        gameSessions.remove(roomId);
        log.info("✅ [GAME_SESSION_CLEANED] Room: {} game session removed", inviteCode);
    }

    public void resendCurrentQuestionToUser(String roomId) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();
        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();
        if (q == null) return;

        messagingTemplate.convertAndSend(
                "/topic/room/" + inviteCode,
                MultiRoomEventResponse.question(
                        q.text(),
                        q.options(),
                        session.getCurrentQuestionIndex()
                )
        );
    }

    @EventListener
    public void handleMultiRoomGameStart(MultiRoomGameStartEvent event) {
        startMultiRoomGame(event.roomId());
    }
}
