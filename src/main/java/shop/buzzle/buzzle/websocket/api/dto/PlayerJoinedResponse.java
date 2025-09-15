package shop.buzzle.buzzle.websocket.api.dto;

public record PlayerJoinedResponse(
        String type,
        String email,
        String name,
        String picture,
        String message
) {
    public static PlayerJoinedResponse of(String email, String name, String picture) {
        return new PlayerJoinedResponse(
                "PLAYER_JOINED",
                email,
                name,
                picture,
                name + "님이 입장했습니다."
        );
    }
}