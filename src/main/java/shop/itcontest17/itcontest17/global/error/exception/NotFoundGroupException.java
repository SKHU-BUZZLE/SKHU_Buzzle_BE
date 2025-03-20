package shop.itcontest17.itcontest17.global.error.exception;

public abstract class NotFoundGroupException extends RuntimeException{
    public NotFoundGroupException(String message) {
        super(message);
    }
}