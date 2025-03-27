package shop.itcontest17.itcontest17.websocket.api.dto;

public record WebSocketResponse() {

    public record Send(String sender, String message) {
        public static Send of(String sender, String message) {
            return new Send(sender, message);
        }
    }

    public record SendEvent(String event) {
        public static SendEvent of(String event) {
            return new SendEvent(event);
        }
    }

    public record SendQuestion(String question) {
        public static SendQuestion of(String question) {
            return new SendQuestion(question);
        }
    }

    public record SendAnswer(String answer) {
        public static SendAnswer of(String answer) {
            return new SendAnswer(answer);
        }
    }

    public record SendHint(String hint) {
        public static SendHint of(String hint) {
            return new SendHint(hint);
        }
    }
}
