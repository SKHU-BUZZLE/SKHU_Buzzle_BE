package shop.itcontest17.itcontest17.global.error.exception;

public abstract class InvalidGroupException extends RuntimeException{
    public InvalidGroupException(String message) {
        super(message);
    }
}