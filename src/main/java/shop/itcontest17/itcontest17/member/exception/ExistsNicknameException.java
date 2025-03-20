package shop.itcontest17.itcontest17.member.exception;

import shop.itcontest17.itcontest17.global.error.exception.InvalidGroupException;

public class ExistsNicknameException extends InvalidGroupException {
    public ExistsNicknameException(String message) {
        super(message);
    }

    public ExistsNicknameException() {
        this("이미 사용중인 닉네임 입니다.");
    }
}