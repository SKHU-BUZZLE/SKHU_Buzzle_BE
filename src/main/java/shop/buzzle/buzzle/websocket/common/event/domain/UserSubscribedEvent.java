package shop.buzzle.buzzle.websocket.common.event.domain;

/**
 * 사용자가 WebSocket 토픽을 구독했을 때 발생하는 이벤트
 */
public record UserSubscribedEvent(
        String userEmail,
        String roomId,
        String destination,
        String sessionId
) {
}
