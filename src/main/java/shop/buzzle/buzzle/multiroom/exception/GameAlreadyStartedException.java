package shop.buzzle.buzzle.multiroom.exception;

import shop.buzzle.buzzle.global.error.exception.InvalidGroupException;

public class GameAlreadyStartedException extends InvalidGroupException {
    public GameAlreadyStartedException() {
        super("이미 시작된 게임입니다.");
    }
}