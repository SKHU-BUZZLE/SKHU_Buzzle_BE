package shop.buzzle.buzzle.websocket.common.event.domain;

import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;

/**
 * 게임 종료를 알리는 이벤트
 *
 * @param roomId      게임방 ID
 * @param inviteCode  초대 코드 (초대방의 경우)
 * @param gameType    게임 타입 (INVITE or RANDOM)
 * @param gameEndData 게임 결과 데이터 (DTO)
 * @param winnerEmail 우승자 이메일
 * @param hasTie      동점 여부
 */
import java.time.Instant;

public record GameEndedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Object gameEndData,
        String winnerEmail,
        boolean hasTie,
        Instant timestamp
) implements GameEvent {

    public GameEndedEvent(String roomId, String inviteCode, GameType gameType, Object gameEndData, String winnerEmail, boolean hasTie) {
        this(roomId, inviteCode, gameType, gameEndData, winnerEmail, hasTie, Instant.now());
    }
}