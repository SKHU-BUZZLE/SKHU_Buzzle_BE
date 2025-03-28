package shop.itcontest17.itcontest17.websocket.api.dto;

public record WebSocketResponse() {

    public record Send(String sender, String message) {
        public static Send of(String sender, String message) {
            return new Send(sender, message);
        }
    }
}
