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
        // ✅ TokenProvider와 동일한 방식으로 Hex 문자열 → byte[] 변환 후 Key 생성
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
        String token = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("authorization");

        log.info("🔒 WebSocket 인증 시도 - {}", token);

        if (token != null && validate(token)) {
            Claims claims = parseToken(token);
            attributes.put("userEmail", claims.getSubject());
            log.info("✅ WebSocket 인증 성공 - {}", claims.getSubject());
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
        // 필요시 로그 등 후처리
    }
}
