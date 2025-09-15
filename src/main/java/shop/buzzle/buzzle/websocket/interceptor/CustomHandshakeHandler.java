package shop.buzzle.buzzle.websocket.interceptor;

import shop.buzzle.buzzle.websocket.interceptor.StompPrincipal;

import java.security.Principal;
import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        // attributes는 HandshakeInterceptor에서 설정한 것
        String userEmail = (String) attributes.get("userEmail");

        // Principal로 래핑해서 반환
        return new StompPrincipal(userEmail);
    }
}