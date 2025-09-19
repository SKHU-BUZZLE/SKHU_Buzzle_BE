package shop.buzzle.buzzle.multiroom.application;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomCreateReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomJoinReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomCreateResDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomInfoResDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.InviteCodeValidationResDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.GameEndResponseDto;
import shop.buzzle.buzzle.multiroom.domain.MultiRoom;
import shop.buzzle.buzzle.multiroom.exception.MultiRoomFullException;
import shop.buzzle.buzzle.multiroom.exception.MultiRoomNotFoundException;
import shop.buzzle.buzzle.multiroom.exception.InvalidInviteCodeException;
import shop.buzzle.buzzle.multiroom.exception.GameAlreadyStartedException;
import shop.buzzle.buzzle.multiroom.event.MultiRoomGameStartEvent;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class
MultiRoomService {

    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, MultiRoom> roomsByRoomId = new ConcurrentHashMap<>();
    private final Map<String, String> inviteCodeToRoomId = new ConcurrentHashMap<>();

    private static final String INVITE_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private final SecureRandom random = new SecureRandom();

    public MultiRoomCreateResDto createRoom(String hostEmail, MultiRoomCreateReqDto request) {
        Member host = memberRepository.findByEmail(hostEmail)
                .orElseThrow(MemberNotFoundException::new);

        String roomId = UUID.randomUUID().toString();
        String inviteCode = generateInviteCode();

        MultiRoom room = new MultiRoom(
                roomId,
                inviteCode,
                request.maxPlayers(),
                request.category(),
                request.quizCount()
        );

        roomsByRoomId.put(roomId, room);
        inviteCodeToRoomId.put(inviteCode, roomId);

        return new MultiRoomCreateResDto(
                inviteCode,
                request.maxPlayers(),
                request.category(),
                request.quizCount(),
                host.getName()
        );
    }

    public MultiRoomInfoResDto joinRoom(String playerEmail, MultiRoomJoinReqDto request) {
        String roomId = inviteCodeToRoomId.get(request.inviteCode());
        if (roomId == null) {
            throw new InvalidInviteCodeException();
        }

        MultiRoom room = roomsByRoomId.get(roomId);
        if (room == null) {
            throw new MultiRoomNotFoundException();
        }

        if (room.isGameStarted()) {
            throw new GameAlreadyStartedException();
        }

        if (room.isFull()) {
            throw new MultiRoomFullException();
        }

        Member player = memberRepository.findByEmail(playerEmail)
                .orElseThrow(MemberNotFoundException::new);

        // 첫 유저가 방장이 됨
        if (room.getCurrentPlayerCount() == 0) {
            room.setHost(playerEmail);
        }

        boolean added = room.addPlayer(playerEmail);
        if (!added) {
            throw new MultiRoomFullException();
        }

        return buildRoomInfo(room);
    }

    public void leaveRoom(String roomId, String playerEmail) {
        MultiRoom room = roomsByRoomId.get(roomId);
        if (room == null) {
            return;
        }

        if (room.isHost(playerEmail)) {
            disbandRoom(roomId);
        } else {
            room.removePlayer(playerEmail);
        }
    }

    public void startGame(String roomId, String hostEmail) {
        MultiRoom room = roomsByRoomId.get(roomId);
        if (room == null) {
            throw new MultiRoomNotFoundException();
        }

        if (!room.isHost(hostEmail)) {
            throw new IllegalArgumentException("방장만 게임을 시작할 수 있습니다.");
        }

        room.startGame();
        eventPublisher.publishEvent(new MultiRoomGameStartEvent(roomId));
    }

    public MultiRoom getRoom(String roomId) {
        return roomsByRoomId.get(roomId);
    }

    public void disbandRoomAfterGame(String roomId) {
        disbandRoom(roomId);
    }

    public InviteCodeValidationResDto validateInviteCode(String inviteCode) {
        // 초대코드로 방 ID 찾기
        String roomId = inviteCodeToRoomId.get(inviteCode);
        if (roomId == null) {
            return InviteCodeValidationResDto.invalid("존재하지 않는 초대코드입니다.");
        }

        // 방 정보 확인
        MultiRoom room = roomsByRoomId.get(roomId);
        if (room == null) {
            // 초대코드는 있지만 방이 없는 경우 (데이터 일관성 문제)
            inviteCodeToRoomId.remove(inviteCode);
            return InviteCodeValidationResDto.invalid("방을 찾을 수 없습니다.");
        }

        // 게임 시작 여부 확인
        if (room.isGameStarted()) {
            return InviteCodeValidationResDto.invalid("이미 게임이 시작된 방입니다.");
        }

        // 방이 가득 찬 경우
        if (room.isFull()) {
            return InviteCodeValidationResDto.invalid("방이 가득 찼습니다.");
        }

        // 방장 정보 가져오기
        String hostEmail = room.getHostEmail();
        String hostName = "알 수 없음";
        if (hostEmail != null) {
            hostName = memberRepository.findByEmail(hostEmail)
                    .map(Member::getName)
                    .orElse("알 수 없음");
        }

        return InviteCodeValidationResDto.valid(
                roomId,
                inviteCode,
                hostName,
                room.getCurrentPlayerCount(),
                room.getMaxPlayers(),
                room.getCategory(),
                room.getQuizCount()
        );
    }

    private void disbandRoom(String roomId) {
        MultiRoom room = roomsByRoomId.remove(roomId);
        if (room != null) {
            inviteCodeToRoomId.remove(room.getInviteCode());
        }
    }

    public MultiRoomInfoResDto buildRoomInfo(MultiRoom room) {
        List<MultiRoomInfoResDto.PlayerInfoDto> players = room.getPlayerEmails().stream()
                .map(email -> {
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(MemberNotFoundException::new);
                    return new MultiRoomInfoResDto.PlayerInfoDto(
                            member.getEmail(),
                            member.getName(),
                            member.getPicture() != null ? member.getPicture() : "",
                            room.isHost(email)
                    );
                })
                .toList();

        return new MultiRoomInfoResDto(
                room.getRoomId(),
                room.getInviteCode(),
                getHostName(room.getHostEmail()),
                room.getMaxPlayers(),
                room.getCurrentPlayerCount(),
                room.getCategory(),
                room.getQuizCount(),
                room.isGameStarted(),
                room.canStartGame(),
                players
        );
    }

    private String getHostName(String hostEmail) {
        if (hostEmail == null) {
            return "Unknown";
        }
        return memberRepository.findByEmail(hostEmail)
                .map(Member::getName)
                .orElse("Unknown");
    }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CODE_CHARS.charAt(random.nextInt(INVITE_CODE_CHARS.length())));
        }

        String inviteCode = code.toString();

        if (inviteCodeToRoomId.containsKey(inviteCode)) {
            return generateInviteCode();
        }

        return inviteCode;
    }

    public GameEndResponseDto.GameEndData createGameEndRanking(Map<String, Integer> scores, List<String> allPlayerEmails) {
        // 모든 플레이어 정보 수집 (점수가 없는 플레이어는 0점)
        List<GameEndResponseDto.PlayerRanking> rankings = allPlayerEmails.stream()
                .map(email -> {
                    Member member = memberRepository.findByEmail(email)
                            .orElseThrow(MemberNotFoundException::new);
                    int score = scores.getOrDefault(email, 0);

                    return new GameEndResponseDto.PlayerRanking(
                            0, // rank는 나중에 설정
                            member.getEmail(),
                            member.getName(),
                            member.getPicture() != null ? member.getPicture() : "",
                            score,
                            false // isWinner는 나중에 설정
                    );
                })
                .sorted((p1, p2) -> Integer.compare(p2.score(), p1.score())) // 점수 내림차순
                .collect(Collectors.toList());

        // 랭킹 설정 및 동점 처리
        int currentRank = 1;
        int maxScore = rankings.isEmpty() ? 0 : rankings.get(0).score();
        boolean hasTie = false;

        // 1등이 여러 명인지 확인
        long winnersCount = rankings.stream().filter(p -> p.score() == maxScore).count();
        if (winnersCount > 1) {
            hasTie = true;
        }

        List<GameEndResponseDto.PlayerRanking> finalRankings = new ArrayList<>();

        for (int i = 0; i < rankings.size(); i++) {
            GameEndResponseDto.PlayerRanking player = rankings.get(i);

            // 이전 플레이어와 점수가 다르면 랭킹 업데이트
            if (i > 0 && player.score() != rankings.get(i - 1).score()) {
                currentRank = i + 1;
            }

            // 1등인지 확인 (최고 점수와 같은 점수)
            boolean isWinner = player.score() == maxScore && maxScore > 0;

            finalRankings.add(new GameEndResponseDto.PlayerRanking(
                    currentRank,
                    player.email(),
                    player.name(),
                    player.picture(),
                    player.score(),
                    isWinner
            ));
        }

        return new GameEndResponseDto.GameEndData(finalRankings, hasTie);
    }
}