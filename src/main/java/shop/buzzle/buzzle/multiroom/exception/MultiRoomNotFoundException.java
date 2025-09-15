package shop.buzzle.buzzle.multiroom.exception;

import shop.buzzle.buzzle.global.error.exception.NotFoundGroupException;

public class MultiRoomNotFoundException extends NotFoundGroupException {
    public MultiRoomNotFoundException() {
        super("존재하지 않는 멀티플레이어 방입니다.");
    }
}