package shop.buzzle.buzzle.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.ExternalDocumentation;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI nongDamAPI() {
        Info info = new Info()
                .title("버즐 API")
                .description("버즐 API 명세서\n\n" +
                        "## WebSocket 연결 정보\n" +
                        "- **연결 URL**: wss://dev-buzzle2.store/chat 혹은 운영 서버면 wss://buzzle2.store/chat\n" +
                        "- **Protocol**: STOMP over WebSocket\n" +
                        "- **구독 경로**: `/topic/game/{roomId}` (모든 게임 이벤트)\n\n" +
                        "## WebSocket 이벤트 타입\n\n" +
                        "### 📨 Client → Server (발행)\n" +
                        "1. **일반 메시지**: `/app/room/{roomId}`\n" +
                        "2. **게임 메시지**: `/app/game/{roomId}`\n" +
                        "3. **퀴즈 답안**: `/app/game/{roomId}/answer`(지금 로직상은 퀴즈 답안 전송하는 로직만 있으니 여기로 보내면 됨)\n\n" +
                        "### 📡 Server → Client (수신 이벤트)\n" +
                        "**PLAYER_JOINED** - 플레이어 입장 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"PLAYER_JOINED\",\n" +
                        "  \"email\": \"user@example.com\",\n" +
                        "  \"name\": \"사용자명\",\n" +
                        "  \"picture\": \"프로필이미지URL\"\n" +
                        "}\n" +
                        "```\n\n" +
                        "**PLAYER_LEFT** - 플레이어 퇴장 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"PLAYER_LEFT\",\n" +
                        "  \"message\": \"user@example.com님이 퇴장했습니다.\"\n" +
                        "}\n" +
                        "```\n\n" +
                        "**QUESTION** - 퀴즈 문제 전송 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"QUESTION\",\n" +
                        "  \"question\": \"문제 내용\",\n" +
                        "  \"options\": [\"선택지1\", \"선택지2\", \"선택지3\", \"선택지4\"],\n" +
                        "  \"questionIndex\": 0\n" +
                        "}\n" +
                        "```\n\n" +
                        "**ANSWER_RESULT** - 답변 결과 전송 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"ANSWER_RESULT\",\n" +
                        "  \"message\": \"사용자명님이 정답을 맞췄습니다.\",\n" +
                        "  \"correctIndex\": 2,\n" +
                        "  \"correct\": true\n" +
                        "}\n" +
                        "```\n\n" +
                        "**LEADERBOARD** - 리더보드 업데이트 시(현재 누가 1등인지)\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"LEADERBOARD\",\n" +
                        "  \"currentLeader\": \"user@example.com\",\n" +
                        "  \"scores\": {\n" +
                        "    \"user1@example.com\": 1,\n" +
                        "  }\n" +
                        "}\n" +
                        "```\n\n" +
                        "**GAME_END** - 게임 종료 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"GAME_END\",\n" +
                        "  \"message\": \"게임이 종료되었습니다.\",\n" +
                        "  \"winner\": \"user@example.com\"\n" +
                        "}\n" +
                        "```\n\n" +
                        "**LOADING** - 다음 문제 로딩 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"LOADING\",\n" +
                        "  \"message\": \"3초 후 다음 문제가 전송됩니다.\"\n" +
                        "}\n" +
                        "```")
                .version("1.0.0");

        String jwtSchemeName = "JWT TOKEN";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        Server localServer = new Server()
                .url("https://buzzle2.store")
                .description("Local Server");

        return new OpenAPI()
                .info(info)
                .servers(Collections.singletonList(localServer))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    public SwaggerConfig(MappingJackson2HttpMessageConverter converter) {
        var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType("application", "octet-stream"));
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }
}
