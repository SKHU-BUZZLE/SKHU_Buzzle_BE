package shop.buzzle.buzzle.websocket.global.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import shop.buzzle.buzzle.websocket.common.event.domain.UserDisconnectedEvent;
import shop.buzzle.buzzle.websocket.common.event.domain.UserSubscribedEvent;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WSEventListener {

    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String destination = accessor.getDestination();
        String userEmail = (String) sessionAttributes.get("userEmail");
        String sessionId = accessor.getSessionId();

        if (destination == null || userEmail == null) return;

        // 개인 큐 구독은 무시
        if (destination.startsWith("/user/")) {
            return;
        }

        String roomId = parseRoomIdFromDestination(destination);
        if (roomId != null) {
            sessionAttributes.put("roomId", roomId);
            sessionAttributes.put("destination", destination);

            log.info("EVENT PUBLISH: UserSubscribedEvent - User: {}, Room: {}, Destination: {}", userEmail, roomId, destination);
            eventPublisher.publishEvent(new UserSubscribedEvent(userEmail, roomId, destination, sessionId));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) return;

        String roomId = (String) sessionAttributes.get("roomId");
        String userEmail = (String) sessionAttributes.get("userEmail");
        String destination = (String) sessionAttributes.get("destination");
        String sessionId = accessor.getSessionId();

        if (roomId != null && userEmail != null && destination != null) {
            log.info("EVENT PUBLISH: UserDisconnectedEvent - User: {}, Room: {}, Destination: {}", userEmail, roomId, destination);
            eventPublisher.publishEvent(new UserDisconnectedEvent(userEmail, roomId, destination, sessionId));
        }
    }

    private String parseRoomIdFromDestination(String destination) {
        if (destination == null) return null;
        try {
            String[] parts = destination.split("/");
            return parts[parts.length - 1];
        } catch (Exception e) {
            log.error("Error parsing room ID from destination: {}", destination, e);
            return null;
        }
    }
}