package shop.buzzle.buzzle.websocket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import shop.buzzle.buzzle.websocket.interceptor.WebsocketHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String secretKey;

    public WebSocketConfig(@Value("${jwt.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 경로 (브로커 메시지 전달)
        config.enableSimpleBroker("/topic");

        // 클라이언트가 메시지를 보낼 때 붙이는 prefix (Controller 매핑 대상)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 시작하는 엔드포인트
        registry.setErrorHandler(new StompSubProtocolErrorHandler())
                .addEndpoint("/chat")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new WebsocketHandshakeInterceptor(secretKey))
                .withSockJS(); // SockJS fallback 지원
    }
}
