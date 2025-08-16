package shop.buzzle.buzzle.notification.exception;

public class SendFailedException extends RuntimeException {
    public SendFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}