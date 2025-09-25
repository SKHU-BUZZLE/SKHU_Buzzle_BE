package shop.buzzle.buzzle.websocket.random.matchmaking.notification.exception;

public class SendFailedException extends RuntimeException {
    public SendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}