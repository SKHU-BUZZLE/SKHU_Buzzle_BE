package shop.itcontest17.itcontest17.websocket.api.dto;

public record RoomResponse(
        String type, String message
) {

    public static RoomResponse of(String type, String message) {
        return new RoomResponse(type, message);
    }
}
