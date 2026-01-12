package shop.buzzle.buzzle.websocket.invite.application;

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
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InvitedRoomJoinReqDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.invitedRoomEventResDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.GameEndResDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.InvitedRoom;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.GameStartDto;
import shop.buzzle.buzzle.websocket.invite.exception.MultiRoomNotFoundException;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.application.QuizService;
import shop.buzzle.buzzle.quiz.domain.QuizScore;
import shop.buzzle.buzzle.websocket.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.dto.Question;
import shop.buzzle.buzzle.websocket.dto.AnswerResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import shop.buzzle.buzzle.websocket.invite.game.application.InviteGameSession;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiRoomWebSocketService {

    private final InviteRoomService inviteRoomService;
    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    private final Map<String, InviteGameSession> gameSessions = new ConcurrentHashMap<>();
    private final Map<String, Object> roomLocks = new ConcurrentHashMap<>();
    private final Map<String, List<ScheduledFuture<?>>> roomTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    public void joinRoom(String playerEmail, InvitedRoomJoinReqDto request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 널 값 검증
            if (playerEmail == null || playerEmail.trim().isEmpty()) {
                throw new IllegalArgumentException("플레이어 이메일이 비어있습니다.");
            }

            if (request == null) {
                throw new IllegalArgumentException("요청 데이터가 비어있습니다.");
            }

            log.info("🚪 [ROOM_JOIN_START] Player: {}, InviteCode: {}", playerEmail, request.inviteCode());

            var roomInfo = inviteRoomService.joinRoom(playerEmail, request);

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
                    invitedRoomEventResDto.joinedRoom(roomInfo)
            );

            InvitedRoom room = inviteRoomService.getRoom(roomId);
            if (room == null) {
                log.error("❌ [ROOM_JOIN_ERROR] room is null after join. roomId: {}", roomId);
                throw new RuntimeException("방 참가 후 방을 찾을 수 없습니다.");
            }

            Member player = memberRepository.findByEmail(playerEmail)
                    .orElseThrow(() -> new MemberNotFoundException("참가자를 찾을 수 없습니다."));

            // 방 전체에 입장 알림
            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    invitedRoomEventResDto.playerJoined(player)
            );

        } catch (Exception e) {
            log.error("❌ [ROOM_JOIN_ERROR] Player: {}, Error: {}", playerEmail, e.getMessage(), e);

            if (playerEmail != null && messagingTemplate != null) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            playerEmail,
                            "/queue/room",
                            invitedRoomEventResDto.error("방 참가 실패: " + e.getMessage())
                    );
                } catch (Exception sendError) {
                    log.error("❌ [ERROR_SEND_FAILED] Failed to send error message to user: {}", sendError.getMessage());
                }
            }
        }
    }

    public void leaveRoom(String roomId, String playerEmail) {
        try {
            InvitedRoom room = inviteRoomService.getRoom(roomId);
            if (room == null) return;

            String inviteCode = room.getInviteCode();
            boolean isHost = room.isHost(playerEmail);

            inviteRoomService.leaveRoom(roomId, playerEmail);

            // 방장이 나가면 방 폭파, 아니면 퇴장 알림
            if (isHost) {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + inviteCode,
                        invitedRoomEventResDto.message("방장이 퇴장하여 방이 해체되었습니다.")
                );
                gameSessions.remove(roomId);
                roomLocks.remove(roomId);
                cancelRoomTimers(roomId);
                log.info("❌ [ROOM_DISBANDED] Host left, InviteCode: {} disbanded", inviteCode);
            } else {
                Member player = memberRepository.findByEmail(playerEmail)
                        .orElse(null);
                String playerName = player != null ? player.getName() : playerEmail;

                messagingTemplate.convertAndSend(
                        "/topic/room/" + inviteCode,
                        invitedRoomEventResDto.playerLeft(playerName, playerEmail)
                );
                log.info("✅ [PLAYER_LEFT] Player: {} ({}), InviteCode: {}", playerName, playerEmail, inviteCode);
            }
        } catch (Exception e) {
            log.error("❌ [LEAVE_ROOM_ERROR] Player: {}, Error: {}", playerEmail, e.getMessage());
        }
    }

    public void startGame(String roomId, String hostEmail) {
        try {
            InvitedRoom room = inviteRoomService.getRoom(roomId);
            if (room == null) {
                throw new MultiRoomNotFoundException();
            }

            String inviteCode = room.getInviteCode();

            log.info("✅ [GAME_START_REQUEST] Host: {}, Room: {}, Players: {}/{}",
                    hostEmail, inviteCode, room.getCurrentPlayerCount(), room.getMaxPlayers());

            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    invitedRoomEventResDto.gameStartNotification()
            );

            inviteRoomService.startGame(roomId, hostEmail);

        } catch (Exception e) {
            InvitedRoom room = inviteRoomService.getRoom(roomId);
            String inviteCode = room != null ? room.getInviteCode() : "unknown";

            log.error("❌ [GAME_START_ERROR] Room: {}, Error: {}", inviteCode, e.getMessage());

            messagingTemplate.convertAndSend(
                    "/topic/room/" + inviteCode,
                    invitedRoomEventResDto.error("게임 시작 실패: " + e.getMessage())
            );
        }
    }

    @Transactional
    public void startMultiRoomGame(String roomId) {
        InvitedRoom room = inviteRoomService.getRoom(roomId);
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

        InviteGameSession session = new InviteGameSession(
                roomId,
                questions,
                room.getPlayerEmails(),
                room.getCategory()
        );

        gameSessions.put(roomId, session);

        Map<String, Object> gameStartPayload = Map.of(
            "type", "GAME_START",
            "totalQuestions", session.getTotalQuestions(),
            "countdownSeconds", 0
        );
        messagingTemplate.convertAndSend("/topic/room/" + inviteCode, gameStartPayload);

        log.info("✅ [GAME_COUNTDOWN] Room: {}, Starting in 0.1 seconds...", inviteCode);

        CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS).execute(() -> {
            sendCurrentQuestion(roomId);
        });
    }

    public void sendCurrentQuestion(String roomId) {
        InvitedRoom room = inviteRoomService.getRoom(roomId);
        if (room == null) return;
        String inviteCode = room.getInviteCode();

        InviteGameSession session = gameSessions.get(roomId);
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

        // 타이머가 이미 실행 중이 아닌 경우에만 시작
        if (session.tryStartTimer()) {
            startQuestionTimer(roomId, inviteCode, 10);
        }
    }

    private void startQuestionTimer(String roomId, String inviteCode, int seconds) {
        InviteGameSession session = gameSessions.get(roomId);
        if (session == null) return;

        // 기존 타이머들 취소
        cancelRoomTimers(roomId);

        List<ScheduledFuture<?>> timerTasks = new ArrayList<>();

        // 타이머 카운트다운 스케줄
        for (int i = seconds; i > 0; i--) {
            final int currentSecond = i;
            ScheduledFuture<?> timerTask = scheduler.schedule(() -> {
                // 세션이 이미 제거되었으면 타이머 중단
                InviteGameSession currentSession = gameSessions.get(roomId);
                if (currentSession == null) {
                    log.debug("타이머 중단: 세션이 제거됨 (roomId: {})", roomId);
                    return;
                }

                // 세션이 끝났거나 타이머가 중단되었으면 타이머 중단
                if (currentSession.isFinished() || !currentSession.isTimerRunning()) return;

                Map<String, Object> timerPayload = Map.of(
                    "type", "TIMER",
                    "remainingTime", currentSecond
                );
                messagingTemplate.convertAndSend("/topic/room/" + inviteCode, timerPayload);
            }, seconds - i, TimeUnit.SECONDS);

            timerTasks.add(timerTask);
        }

        // 시간 종료 스케줄
        ScheduledFuture<?> timeUpTask = scheduler.schedule(() -> {
            // 세션이 이미 제거되었으면 타이머 중단
            InviteGameSession currentSession = gameSessions.get(roomId);
            if (currentSession == null) {
                log.debug("타이머 중단: 세션이 제거됨 (roomId: {})", roomId);
                return;
            }

            // 세션이 끝났거나 타이머가 중단되었으면 시간 종료 처리하지 않음
            if (currentSession.isFinished() || !currentSession.isTimerRunning()) return;

            Map<String, Object> timeUpPayload = Map.of(
                "type", "TIME_UP",
                "message", "시간이 종료되었습니다!"
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, timeUpPayload);

            // 시간 초과 시 모든 플레이어의 life 감소
            InvitedRoom room = inviteRoomService.getRoom(roomId);
            if (room != null) {
                for (String playerEmail : room.getPlayerEmails()) {
                    Member member = memberRepository.findByEmail(playerEmail)
                            .orElse(null);
                    if (member != null) {
                        member.decrementLife();
                        memberRepository.save(member);
                        log.info("⏰ [TIMEOUT_LIFE_DECREASED] Player: {} lost 1 life due to timeout, remaining: {}",
                                member.getName(), member.getLife());
                    }
                }
            }

            // 시간 초과 처리
            if (!currentSession.isFinished()) {
                roomLocks.putIfAbsent(roomId, new Object());
                synchronized (roomLocks.get(roomId)) {
                    // 마지막 문제인 경우 바로 게임 종료
                    if (currentSession.getCurrentQuestionIndex() >= currentSession.getTotalQuestions() - 1) {
                        currentSession.tryNextQuestion(); // 게임을 finished 상태로 만들기
                        handleMultiRoomGameEnd(roomId, currentSession);
                        roomLocks.remove(roomId);
                    } else {
                        // 마지막 문제가 아닌 경우 다음 문제로
                        if (currentSession.tryNextQuestion()) {
                            if (currentSession.isFinished()) {
                                handleMultiRoomGameEnd(roomId, currentSession);
                                roomLocks.remove(roomId);
                            } else {
                                Map<String, Object> loadingPayload = Map.of(
                                    "type", "LOADING",
                                    "message", "3초 후 다음 문제가 전송됩니다."
                                );
                                messagingTemplate.convertAndSend("/topic/room/" + inviteCode, loadingPayload);

                                scheduler.schedule(() -> {
                                    synchronized (roomLocks.get(roomId)) {
                                        sendCurrentQuestion(roomId);
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
            log.info("⏹️ [TIMERS_CANCELLED] Room: {} - {} timers cancelled", roomId, timers.size());
        }
    }

    @Transactional
    public void receiveMultiRoomAnswer(String roomId, String email, AnswerRequest answerRequest) {
        InvitedRoom room = inviteRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();
        InviteGameSession session = gameSessions.get(roomId);
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
            AnswerResponse answerResponse = AnswerResponse.of(
                email,
                displayName,
                isCorrect,
                String.valueOf(correctIndex),
                String.valueOf(answerRequest.index())
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, answerResponse);

            if (!isCorrect) {
                // 틀린 답안 제출 시 life 감소
                member.decrementLife();
                log.info("💔 [LIFE_DECREASED] Player: {} submitted wrong answer, lost 1 life, remaining: {}",
                        displayName, member.getLife());
                return;
            }

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

            Map<String, Integer> currentScores = session.getCurrentScores();

            // 이메일 -> 이름 매핑 생성
            Map<String, String> emailToName = new HashMap<>();
            for (String userEmail : currentScores.keySet()) {
                Member user = memberRepository.findByEmail(userEmail)
                        .orElseThrow(MemberNotFoundException::new);
                emailToName.put(userEmail, user.getName());
            }

            Map<String, Object> leaderboardPayload = Map.of(
                "type", "LEADERBOARD",
                "currentLeader", currentLeaderName,
                "currentLeaderEmail", currentLeaderEmail,
                "scores", currentScores,
                "emailToName", emailToName
            );
            messagingTemplate.convertAndSend("/topic/room/" + inviteCode, leaderboardPayload);

            if (session.tryNextQuestion()) {
                // 다음 문제로 넘어갈 때 현재 타이머 즉시 중단
                session.stopTimer();
                cancelRoomTimers(roomId);

                if (session.isFinished()) {
                    log.info("🏁 [GAME_FINISHED] Room: {}, Moving to game end", inviteCode);
                    handleMultiRoomGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    log.info("⏭️ [NEXT_QUESTION] Room: {}, Question {}/{} completed, preparing next question",
                            inviteCode, session.getCurrentQuestionIndex(), session.getTotalQuestions());

                    // 타이머 중단 알림
                    Map<String, Object> timerStopPayload = Map.of(
                        "type", "TIMER_STOP",
                        "message", "정답! 다음 문제로 이동합니다."
                    );
                    messagingTemplate.convertAndSend("/topic/room/" + inviteCode, timerStopPayload);

                    Map<String, Object> loadingPayload = Map.of(
                        "type", "LOADING",
                        "message", "3초 후 다음 문제가 전송됩니다."
                    );
                    messagingTemplate.convertAndSend("/topic/room/" + inviteCode, loadingPayload);

                    scheduler.schedule(() -> {
                        synchronized (roomLocks.get(roomId)) {
                            sendCurrentQuestion(roomId);
                        }
                    }, 3, TimeUnit.SECONDS);
                }
            }
        }
    }

    private void handleMultiRoomGameEnd(String roomId, InviteGameSession session) {
        InvitedRoom room = inviteRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();

        // 랭킹 데이터 생성
        Map<String, Integer> scores = session.getCurrentScores();
        List<String> allPlayerEmails = room.getPlayerEmails();
        GameEndResDto.GameEndData gameEndData = inviteRoomService.createGameEndRanking(scores, allPlayerEmails);

        // 우승자에게 점수 부여
        String winner = session.getWinner();
        if (winner != null) {
            Member member = memberRepository.findByEmail(winner)
                    .orElseThrow(MemberNotFoundException::new);
            member.incrementStreak(QuizScore.MULTI_SCORE.getScore());
        }

        // 로그 출력
        if (gameEndData.hasTie()) {
            log.info("🤝 [GAME_TIE] Room: {}, Multiple winners with same score", inviteCode);
        } else if (winner != null) {
            Member member = memberRepository.findByEmail(winner)
                    .orElseThrow(MemberNotFoundException::new);
            log.info("🏆 [GAME_WINNER] Room: {}, Winner: {}", inviteCode, member.getName());
        }

        // 랭킹 정보와 함께 게임 종료 메시지 전송
        invitedRoomEventResDto gameEndResponse = invitedRoomEventResDto.gameEndWithRanking(gameEndData);
        messagingTemplate.convertAndSend("/topic/room/" + inviteCode, gameEndResponse);

        // 게임 세션 정리
        gameSessions.remove(roomId);

        // 타이머 정리
        cancelRoomTimers(roomId);

        // 방 폭파
        inviteRoomService.disbandRoomAfterGame(roomId);

        log.info("💥 [ROOM_DISBANDED] Room: {} disbanded after game completion", inviteCode);
    }

    public void forceCleanupRoom(String roomId) {
        log.info("🧹 초대 방 {} 강제 정리 시작 (모든 플레이어 퇴장)", roomId);

        // 타이머 취소 및 제거
        cancelRoomTimers(roomId);

        // 게임 세션 제거
        InviteGameSession session = gameSessions.remove(roomId);
        if (session != null) {
            log.info("  ↳ 게임 세션 제거됨 (현재 문제: {}/{})",
                session.getCurrentQuestionIndex() + 1,
                session.getTotalQuestions());
        }

        // 락 제거
        Object lock = roomLocks.remove(roomId);
        if (lock != null) {
            log.info("  ↳ 락 제거됨");
        }

        // 방 제거
        inviteRoomService.disbandRoomAfterGame(roomId);

        log.info("✅ 초대 방 {} 정리 완료", roomId);
    }

    public void resendCurrentQuestionToUser(String roomId) {
        InvitedRoom room = inviteRoomService.getRoom(roomId);
        if (room == null) return;

        String inviteCode = room.getInviteCode();
        InviteGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();
        if (q == null) return;

        messagingTemplate.convertAndSend(
                "/topic/room/" + inviteCode,
                invitedRoomEventResDto.question(
                        q.text(),
                        q.options(),
                        session.getCurrentQuestionIndex()
                )
        );
    }

    @EventListener
    public void handleMultiRoomGameStart(GameStartDto event) {
        startMultiRoomGame(event.roomId());
    }
}
