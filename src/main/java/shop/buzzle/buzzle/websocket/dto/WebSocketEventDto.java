package shop.buzzle.buzzle.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketEventDto(
        MessageType type,
        String message,
        Object data
) {

    // 타이머 이벤트
    public static WebSocketEventDto timer(int remainingTime) {
        return new WebSocketEventDto(MessageType.TIMER, "", Map.of("remainingTime", remainingTime));
    }

    // 시간 종료 이벤트
    public static WebSocketEventDto timeUp() {
        return new WebSocketEventDto(MessageType.TIME_UP, "시간이 종료되었습니다!", null);
    }

    // 로딩 이벤트
    public static WebSocketEventDto loading(String message) {
        return new WebSocketEventDto(MessageType.LOADING, message, null);
    }

    // 타이머 중단 이벤트
    public static WebSocketEventDto timerStop() {
        return new WebSocketEventDto(MessageType.TIMER_STOP, "정답! 다음 문제로 이동합니다.", null);
    }

    // 플레이어 퇴장 이벤트
    public static WebSocketEventDto playerLeft(String email, String name) {
        return new WebSocketEventDto(MessageType.PLAYER_LEFT, name + "님이 퇴장했습니다.", Map.of("email", email, "name", name));
    }
}
