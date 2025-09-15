package shop.buzzle.buzzle.multiroom.exception;

import shop.buzzle.buzzle.global.error.exception.InvalidGroupException;

public class InvalidInviteCodeException extends InvalidGroupException {
    public InvalidInviteCodeException() {
        super("유효하지 않은 초대 코드입니다.");
    }
}