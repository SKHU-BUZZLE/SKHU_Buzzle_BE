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
import shop.buzzle.buzzle.multiroom.domain.MultiRoom;
import shop.buzzle.buzzle.multiroom.exception.MultiRoomFullException;
import shop.buzzle.buzzle.multiroom.exception.MultiRoomNotFoundException;
import shop.buzzle.buzzle.multiroom.exception.InvalidInviteCodeException;
import shop.buzzle.buzzle.multiroom.exception.GameAlreadyStartedException;
import shop.buzzle.buzzle.multiroom.event.MultiRoomGameStartEvent;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public MultiRoomInfoResDto getRoomInfo(String roomId) {
        MultiRoom room = roomsByRoomId.get(roomId);
        if (room == null) {
            throw new MultiRoomNotFoundException();
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
}