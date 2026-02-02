package shop.buzzle.buzzle.websocket.common.event.domain;

import shop.buzzle.buzzle.websocket.invite.api.dto.response.GameEndResDto;

import java.time.Instant;

public record GameEndedEvent(
        String roomId,
        String inviteCode,
        GameType gameType,
        Instant timestamp,
        GameEndResDto.GameEndData gameEndData,
        String winnerEmail,
        boolean hasTie
) implements GameEvent {

    public GameEndedEvent(String roomId, String inviteCode, GameType gameType,
                          GameEndResDto.GameEndData gameEndData, String winnerEmail, boolean hasTie) {
        this(roomId, inviteCode, gameType, Instant.now(), gameEndData, winnerEmail, hasTie);
    }
}
