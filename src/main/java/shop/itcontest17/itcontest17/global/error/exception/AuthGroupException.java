package shop.itcontest17.itcontest17.global.error.exception;

public abstract class AuthGroupException extends RuntimeException{
    public AuthGroupException(String message) {
        super(message);
    }
}