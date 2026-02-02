package shop.buzzle.buzzle.websocket.invite.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.websocket.common.event.domain.*;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
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
import shop.buzzle.buzzle.websocket.invite.game.application.AnswerResult;
import shop.buzzle.buzzle.websocket.invite.game.application.InviteGameSession;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiRoomWebSocketService {

    private final InviteRoomService inviteRoomService;
    private final QuizService quizService;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

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

            // 개인에게 방 참가 성공 이벤트 전송
            eventPublisher.publishEvent(new NotificationEvent(
                    playerEmail,
                    "/queue/room",
                    invitedRoomEventResDto.joinedRoom(roomInfo),
                    GameType.INVITE
            ));

            InvitedRoom room = inviteRoomService.getRoom(roomId);
            if (room == null) {
                log.error("❌ [ROOM_JOIN_ERROR] room is null after join. roomId: {}", roomId);
                throw new RuntimeException("방 참가 후 방을 찾을 수 없습니다.");
            }

            Member player = memberRepository.findByEmail(playerEmail)
                    .orElseThrow(() -> new MemberNotFoundException("참가자를 찾을 수 없습니다."));

            // 방 전체에 입장 알림 이벤트 발행
            eventPublisher.publishEvent(new PlayerJoinedEvent(
                    roomId,
                    inviteCode,
                    GameType.INVITE,
                    player.getEmail(),
                    player.getName(),
                    player.getPicture()
            ));

        } catch (Exception e) {
            log.error("❌ [ROOM_JOIN_ERROR] Player: {}, Error: {}", playerEmail, e.getMessage(), e);

            // 개인에게 에러 발생 이벤트 전송
            if (playerEmail != null) {
                eventPublisher.publishEvent(new NotificationEvent(
                        playerEmail,
                        "/queue/room",
                        invitedRoomEventResDto.error("방 참가 실패: " + e.getMessage()),
                        GameType.INVITE
                ));
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

            Member player = memberRepository.findByEmail(playerEmail)
                    .orElse(null);
            String playerName = player != null ? player.getName() : playerEmail;

            eventPublisher.publishEvent(new PlayerLeftEvent(
                    roomId,
                    inviteCode,
                    GameType.INVITE,
                    playerEmail,
                    playerName,
                    isHost
            ));

            if (isHost) {
                gameSessions.remove(roomId);
                roomLocks.remove(roomId);
                cancelRoomTimers(roomId);
                log.info("❌ [ROOM_DISBANDED] Host left, InviteCode: {} disbanded", inviteCode);
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

            eventPublisher.publishEvent(new RoomNotificationEvent(
                    null,
                    inviteCode,
                    invitedRoomEventResDto.gameStartNotification(),
                    GameType.INVITE
            ));

            inviteRoomService.startGame(roomId, hostEmail);

        } catch (Exception e) {
            InvitedRoom room = inviteRoomService.getRoom(roomId);
            String inviteCode = room != null ? room.getInviteCode() : "unknown";

            log.error("❌ [GAME_START_ERROR] Room: {}, Error: {}", inviteCode, e.getMessage());

            eventPublisher.publishEvent(new RoomNotificationEvent(
                    null,
                    inviteCode,
                    invitedRoomEventResDto.error("게임 시작 실패: " + e.getMessage()),
                    GameType.INVITE
            ));
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

        // [이벤트 발행] 게임 시작 이벤트
        eventPublisher.publishEvent(new GameStartedEvent(
                roomId, inviteCode, GameType.INVITE,
                room.getPlayerEmails(), session.getTotalQuestions(), room.getCategory()
        ));

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

        // [이벤트 발행] 문제 전송 이벤트
        eventPublisher.publishEvent(new QuestionSentEvent(
                roomId, inviteCode, GameType.INVITE,
                q.text(), q.options(), session.getCurrentQuestionIndex()
        ));

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

                // [이벤트 발행] 타이머 틱 이벤트
                eventPublisher.publishEvent(new TimerTickEvent(roomId, inviteCode, GameType.INVITE, currentSecond));
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

            // [이벤트 발행] 타이머 만료 이벤트
            eventPublisher.publishEvent(new TimerExpiredEvent(
                    roomId, inviteCode, GameType.INVITE, currentSession.getCurrentQuestionIndex()
            ));

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
                                eventPublisher.publishEvent(new RoomNotificationEvent(
                                        null,
                                        inviteCode,
                                        Map.of("type", "LOADING", "message", "3초 후 다음 문제가 전송됩니다."),
                                        GameType.INVITE
                                ));

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

            // processAnswer를 사용하여 결과 얻기 (POJO 테스트 가능)
            AnswerResult result = session.processAnswer(email, answerRequest.index());

            // [이벤트 발행] 답변 검증 이벤트
            eventPublisher.publishEvent(new AnswerValidatedEvent(
                    roomId, inviteCode, GameType.INVITE,
                    email, displayName, answerRequest.questionIndex(),
                    answerRequest.index(), correctIndex, isCorrect, result.wasFirst()
            ));

            if (!isCorrect) {
                // 틀린 답안 제출 시 life 감소
                member.decrementLife();
                log.info("💔 [LIFE_DECREASED] Player: {} submitted wrong answer, lost 1 life, remaining: {}",
                        displayName, member.getLife());
                return;
            }

            // processAnswer에서 이미 점수 처리됨, 중복 방지를 위해 wasFirst 체크
            if (!result.wasFirst()) {
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

            // [이벤트 발행] 리더보드 갱신 이벤트
            eventPublisher.publishEvent(new LeaderboardUpdatedEvent(
                    roomId, inviteCode, GameType.INVITE,
                    currentLeaderEmail, currentLeaderName, currentScores, emailToName
            ));

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
                    eventPublisher.publishEvent(new RoomNotificationEvent(
                            null,
                            inviteCode,
                            Map.of("type", "TIMER_STOP", "message", "정답! 다음 문제로 이동합니다."),
                            GameType.INVITE
                    ));

                    eventPublisher.publishEvent(new RoomNotificationEvent(
                            null,
                            inviteCode,
                            Map.of("type", "LOADING", "message", "3초 후 다음 문제가 전송됩니다."),
                            GameType.INVITE
                    ));

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

        // [이벤트 발행] 게임 종료 이벤트
        eventPublisher.publishEvent(new GameEndedEvent(
                roomId, inviteCode, GameType.INVITE,
                gameEndData, winner, gameEndData.hasTie()
        ));

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

        eventPublisher.publishEvent(new QuestionSentEvent(
                roomId,
                inviteCode,
                GameType.INVITE,
                q.text(),
                q.options(),
                session.getCurrentQuestionIndex()
        ));
    }

    @EventListener
    public void handleMultiRoomGameStart(GameStartDto event) {
        startMultiRoomGame(event.roomId());
    }
}
