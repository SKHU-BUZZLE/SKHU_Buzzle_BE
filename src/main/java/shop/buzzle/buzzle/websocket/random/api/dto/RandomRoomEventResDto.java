package shop.buzzle.buzzle.websocket.random.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import shop.buzzle.buzzle.websocket.dto.MessageType;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RandomRoomEventResDto(
        MessageType type,
        String message,
        Object data
) {

    public static RandomRoomEventResDto joinedRoom(RandomRoomInfoResDto roomInfo) {
        return new RandomRoomEventResDto(MessageType.JOINED_ROOM, "랜덤 매칭이 완료되어 게임이 시작됩니다!", roomInfo);
    }
}