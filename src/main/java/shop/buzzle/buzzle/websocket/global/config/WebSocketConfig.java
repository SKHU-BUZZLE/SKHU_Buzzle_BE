package shop.buzzle.buzzle.websocket.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import shop.buzzle.buzzle.websocket.global.interceptor.WebsocketHandshakeInterceptor;

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
        // topic은 전체 브로드캐스팅 용
        // queue는 특정 개인에게 보내는 메시지 용
        config.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 때 붙이는 prefix (Controller 매핑 대상)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 클라이언트가 WebSocket 연결을 시작하는 엔드포인트. 웹소켓에 접속하기 위한 맨 처음 관문이라고 할 수 있음.
        registry.setErrorHandler(new StompSubProtocolErrorHandler())
                .addEndpoint("/chat") // ws://{domain}//chat으로 최초의 연결을 요청해야 한다.
                .setAllowedOriginPatterns("*")// cors 잡기. nginx로 추가 구현해놨음.
                .addInterceptors(new WebsocketHandshakeInterceptor(secretKey)) // 유저가 유효한지 jwt용 secretKey로 검사함.
                .setHandshakeHandler(new CustomHandshakeHandler()) // 클라이언트를 식별할 Principal을 생성할 방법 지정.
                .withSockJS(); // SockJS fallback 지원
    }
}
