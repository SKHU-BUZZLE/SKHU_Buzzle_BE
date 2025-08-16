package shop.buzzle.buzzle.auth.exception;

import shop.buzzle.buzzle.global.error.exception.NotFoundGroupException;

public class EmailNotFoundException extends NotFoundGroupException {
    public EmailNotFoundException(String message) {
        super(message);
    }

    public EmailNotFoundException() {
        this("존재하지 않는 이메일 입니다.");
    }
}
