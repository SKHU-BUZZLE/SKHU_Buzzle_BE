package shop.buzzle.buzzle.websocket.invite.exception;

import shop.buzzle.buzzle.global.error.exception.InvalidGroupException;

public class MultiRoomFullException extends InvalidGroupException {
    public MultiRoomFullException() {
        super("방이 가득 찼습니다.");
    }
}