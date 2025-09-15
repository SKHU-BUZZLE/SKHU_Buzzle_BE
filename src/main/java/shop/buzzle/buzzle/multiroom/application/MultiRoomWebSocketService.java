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
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomCreateReqDto;
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

    // ✅ 방 생성
    public void createAndJoinRoom(String hostEmail, MultiRoomCreateReqDto request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            var createResponse = multiRoomService.createRoom(hostEmail, request);
            String roomId = createResponse.roomId();

            headerAccessor.getSessionAttributes().put("roomId", roomId);
            headerAccessor.getSessionAttributes().put("destination", "/topic/room/" + roomId);

            // 1️⃣ 방 생성자 개인 큐로 응답
            log.info("📨 [ROOM_CREATE] send to user={} roomId={} inviteCode={}",
                    hostEmail, roomId, createResponse.inviteCode());

            messagingTemplate.convertAndSendToUser(
                    hostEmail,
                    "/queue/room",
                    MultiRoomEventResponse.roomCreated(
                            roomId,
                            createResponse.inviteCode(),
                            createResponse.hostName(),
                            createResponse.maxPlayers(),
                            request.category(),
                            createResponse.quizCount()
                    )
            );

            // 2️⃣ 방 전체에 알림 (방장 입장 브로드캐스트)
            log.info("📢 [ROOM_CREATE_BROADCAST] host={} joined roomId={}",
                    createResponse.hostName(), roomId);

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    MultiRoomEventResponse.playerJoined(
                            createResponse.hostName(),
                            1,
                            createResponse.maxPlayers(),
                            false
                    )
            );

        } catch (Exception e) {
            log.error("❌ [ROOM_CREATE_ERROR] {}", e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                    hostEmail,
                    "/queue/room",
                    MultiRoomEventResponse.error("방 생성 실패: " + e.getMessage())
            );
        }
    }

    // ✅ 방 참가
    public void joinRoom(String playerEmail, MultiRoomJoinReqDto request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            var roomInfo = multiRoomService.joinRoom(playerEmail, request);
            String roomId = roomInfo.roomId();

            headerAccessor.getSessionAttributes().put("roomId", roomId);
            headerAccessor.getSessionAttributes().put("destination", "/topic/room/" + roomId);

            // 1️⃣ 참가자 본인에게 응답
            log.info("📨 [ROOM_JOIN] send to user={} roomId={} inviteCode={}",
                    playerEmail, roomId, roomInfo.inviteCode());

            messagingTemplate.convertAndSendToUser(
                    playerEmail,
                    "/queue/room",
                    MultiRoomEventResponse.joinedRoom(roomInfo)
            );

            // 2️⃣ 방 전체에 브로드캐스트
            MultiRoom room = multiRoomService.getRoom(roomId);
            if (room != null) {
                Member player = memberRepository.findByEmail(playerEmail)
                        .orElseThrow(MemberNotFoundException::new);

                log.info("📢 [ROOM_JOIN_BROADCAST] {} 님이 방 {} 에 참가 (현재 {} / {})",
                        player.getName(), roomId, room.getCurrentPlayerCount(), room.getMaxPlayers());

                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        MultiRoomEventResponse.playerJoined(
                                player.getName(),
                                room.getCurrentPlayerCount(),
                                room.getMaxPlayers(),
                                room.canStartGame()
                        )
                );
            }

        } catch (Exception e) {
            log.error("❌ [ROOM_JOIN_ERROR] {}", e.getMessage(), e);
            messagingTemplate.convertAndSendToUser(
                    playerEmail,
                    "/queue/room",
                    MultiRoomEventResponse.error("방 참가 실패: " + e.getMessage())
            );
        }
    }

    // 웹소켓으로 방 나가기
    public void leaveRoom(String roomId, String playerEmail) {
        try {
            MultiRoom room = multiRoomService.getRoom(roomId);
            if (room == null) return;

            boolean isHost = room.isHost(playerEmail);
            Member player = memberRepository.findByEmail(playerEmail)
                    .orElseThrow(MemberNotFoundException::new);

            // 방에서 나가기
            multiRoomService.leaveRoom(roomId, playerEmail);

            // 방 전체에 플레이어 퇴장 알림
            if (isHost) {
                messagingTemplate.convertAndSend(
                        "/topic/room/" + roomId,
                        MultiRoomEventResponse.playerLeft(player.getName(), 0, 0, true)
                );
                gameSessions.remove(roomId);
                roomLocks.remove(roomId);
            } else {
                MultiRoom updatedRoom = multiRoomService.getRoom(roomId);
                if (updatedRoom != null) {
                    messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            MultiRoomEventResponse.playerLeft(
                                    player.getName(),
                                    updatedRoom.getCurrentPlayerCount(),
                                    updatedRoom.getMaxPlayers(),
                                    false
                            )
                    );
                }
            }
        } catch (Exception e) {
            // 에러는 조용히 처리 (이미 방을 나간 경우 등)
        }
    }

    // 웹소켓으로 게임 시작
    public void startGame(String roomId, String hostEmail) {
        try {
            multiRoomService.startGame(roomId, hostEmail);
            // 이벤트가 발행되어 게임이 시작됩니다
        } catch (Exception e) {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    MultiRoomEventResponse.error("게임 시작 실패: " + e.getMessage())
            );
        }
    }

    @Transactional
    public void startMultiRoomGame(String roomId) {
        MultiRoom room = multiRoomService.getRoom(roomId);
        if (room == null) {
            throw new MultiRoomNotFoundException();
        }

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


        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                MultiRoomEventResponse.gameStart(session.getTotalQuestions(), 3)
        );

        CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
            sendCurrentQuestion(roomId);
        });
    }

    public void sendCurrentQuestion(String roomId) {
        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();
        if (q == null) return;

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                MultiRoomEventResponse.question(
                        q.text(),
                        q.options(),
                        session.getCurrentQuestionIndex()
                )
        );
    }

    @Transactional
    public void receiveMultiRoomAnswer(String roomId, String email, AnswerRequest answerRequest) {
        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        int submittedIndex = answerRequest.index();
        int clientQuestionIndex = answerRequest.questionIndex();

        if (clientQuestionIndex != session.getCurrentQuestionIndex()) return;

        roomLocks.putIfAbsent(roomId, new Object());

        synchronized (roomLocks.get(roomId)) {
            Question current = session.getCurrentQuestion();
            if (current == null) return;

            boolean isCorrect = current.isCorrectIndex(submittedIndex);

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(MemberNotFoundException::new);
            String displayName = member.getName();

            int correctIndex = Integer.parseInt(current.answerIndex()) - 1;
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    MultiRoomEventResponse.answerResult(displayName, isCorrect, correctIndex)
            );

            if (!isCorrect) return;

            boolean accepted = session.tryAnswerCorrect(email, submittedIndex);
            if (!accepted) return;

            String currentLeader = session.getCurrentLeader();
            Map<String, Integer> currentScores = session.getCurrentScores();
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    MultiRoomEventResponse.leaderboard(currentLeader, currentScores)
            );

            if (session.tryNextQuestion()) {
                if (session.isFinished()) {
                    handleMultiRoomGameEnd(roomId, session);
                    roomLocks.remove(roomId);
                } else {
                    messagingTemplate.convertAndSend(
                            "/topic/room/" + roomId,
                            MultiRoomEventResponse.loading("3초 후 다음 문제가 전송됩니다.")
                    );
                    CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS).execute(() -> {
                        synchronized (roomLocks.get(roomId)) {
                            MultiRoomGameSession currentSession = gameSessions.get(roomId);
                            if (currentSession != null && !currentSession.isFinished()) {
                                sendCurrentQuestion(roomId);
                            }
                        }
                    });
                }
            }
        }
    }

    private void handleMultiRoomGameEnd(String roomId, MultiRoomGameSession session) {
        String winner = session.getWinner();

        if (winner != null) {
            Member member = memberRepository.findByEmail(winner)
                    .orElseThrow(MemberNotFoundException::new);

            member.incrementStreak(QuizScore.MULTI_SCORE.getScore());

            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId,
                    MultiRoomEventResponse.gameEnd(member.getName())
            );
        }

        gameSessions.remove(roomId);
    }

    public void resendCurrentQuestionToUser(String roomId) {
        MultiRoomGameSession session = gameSessions.get(roomId);
        if (session == null || session.isFinished()) return;

        Question q = session.getCurrentQuestion();
        if (q == null) return;

        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
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