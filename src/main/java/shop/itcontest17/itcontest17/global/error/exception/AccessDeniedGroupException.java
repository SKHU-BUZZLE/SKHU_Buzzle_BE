package shop.itcontest17.itcontest17.global.error.exception;

public abstract class AccessDeniedGroupException extends RuntimeException{
    public AccessDeniedGroupException(String message) {
        super(message);
    }
}