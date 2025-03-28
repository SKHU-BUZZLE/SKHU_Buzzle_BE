package shop.itcontest17.itcontest17.websocket.interceptor;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public class WebsocketHandshakeInterceptor implements HandshakeInterceptor {

    private final Key key;

    public WebsocketHandshakeInterceptor(String secretKey) {
        byte[] keyBytes = hexStringToByteArray(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        var queryParams = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        String token = queryParams.getFirst("authorization");
        String roomId = queryParams.getFirst("roomId");

        log.info("🔒 WebSocket 인증 시도 - token: {}, roomId: {}", token, roomId);

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        } else if (token != null && token.startsWith("Bearer%20")) {
            token = token.substring("Bearer%20".length());
        }

        if (token != null && validate(token)) {
            Claims claims = parseToken(token);
            String email = claims.getSubject();

            attributes.put("userEmail", email);
            attributes.put("roomId", roomId);

            log.info("✅ WebSocket 인증 성공 - userEmail: {}, roomId: {}", email, roomId);
            return true;
        }

        log.warn("❌ WebSocket 인증 실패");
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean validate(String token) {
        try {
            return parseToken(token).getExpiration().after(new Date());
        } catch (JwtException e) {
            log.warn("❌ JWT 오류: {}", e.getMessage());
            return false;
        }
    }

    private byte[] hexStringToByteArray(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) (
                    (Character.digit(hex.charAt(i), 16) << 4)
                            + Character.digit(hex.charAt(i + 1), 16)
            );
        }
        return data;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // 후처리 필요 시 사용
    }
}
