package shop.buzzle.buzzle.websocket.common.event.domain;

/**
 * 사용자의 WebSocket 연결이 끊어졌을 때 발생하는 이벤트
 */
public record UserDisconnectedEvent(
        String userEmail,
        String roomId,
        String destination,
        String sessionId
) {
}
