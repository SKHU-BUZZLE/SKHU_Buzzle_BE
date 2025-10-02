package shop.buzzle.buzzle.websocket.random.api.dto;

import shop.buzzle.buzzle.websocket.dto.MessageType;

public record PlayerJoinedResponse(
        MessageType type,
        String email,
        String name,
        String picture,
        String message
) {
    public static PlayerJoinedResponse of(String email, String name, String picture) {
        return new PlayerJoinedResponse(
                MessageType.PLAYER_JOINED,
                email,
                name,
                picture,
                name + "님이 입장했습니다."
        );
    }
}