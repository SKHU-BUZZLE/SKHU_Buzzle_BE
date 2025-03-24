package shop.itcontest17.itcontest17.notification.domain;

import lombok.Getter;

@Getter
public enum NotificationMessage {
    FOLLOW_REQUEST("%s 님으로부터 팔로우 요청이 왔습니다. followerId : %d"),
    FOLLOW_ACCEPT("%s 님이 팔로우 요청을 수락하셨습니다."),
    FOLLOW_REJECT("팔로우 요청이 거절되었습니다."),
    ;

    private final String message;

    NotificationMessage(String message) {
        this.message = message;
    }
}
