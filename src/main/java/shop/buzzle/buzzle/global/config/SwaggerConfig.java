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
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-prod}")
    private String prodServerUrl;

    @Value("${swagger.server-dev}")
    private String devServerUrl;

    @Bean
    public OpenAPI nongDamAPI() {
        Info info = new Info()
                .title("버즐 API")
                .description("버즐 API 명세서\n\n" +
                        "## 1대1 매칭 게임 플로우\n\n" +
                        "### 🎯 랜덤 매칭\n" +
                        "1. **매칭 큐 참가**: `/api/v1/matching/join` API 호출\n" +
                        "2. **큐에서 대기**: 2명이 쌓일 때까지 대기\n" +
                        "3. **자동 매칭**: 2명이 쌓이면 리스너가 자동으로 큐에서 제거\n" +
                        "4. **SSE 메시지**: 각 플레이어에게 `roomId`가 포함된 SSE 메시지 전송\n" +
                        "5. **WebSocket 연결**: 받은 `roomId`로 `/topic/game/{roomId}` 구독\n\n" +
                        "### 👥 친구 초대 매칭\n" +
                        "1. **방 생성 & 초대 코드 생성**: API 호출로 초대 코드 획득\n" +
                        "2. **방장 입장**: 초대 코드로 처음 입장하는 유저가 방장이 됨\n" +
                        "3. **친구 초대**: 초대 코드를 친구에게 전달\n" +
                        "4. **친구 참가**: 초대 코드로 입장 (최대 인원까지)\n" +
                        "5. **게임 이벤트 구독**: 모든 참가자가 `/topic/game/{roomId}` 구독\n" +
                        "6. **게임 시작**: 방장이 시작 버튼을 눌러 퀴즈 게임 시작\n\n" +
                        "## WebSocket 연결 정보\n" +
                        "- **연결 URL**: wss://dev-buzzle2.store/chat 혹은 운영 서버면 wss://buzzle2.store/chat\n" +
                        "- **Protocol**: STOMP over WebSocket\n\n" +
                        "### 🎯 랜덤 매칭 시 구독 경로 (자동 시작)\n" +
                        "- **SSE 연결**: 매칭 완료 시 roomId 수신용\n" +
                        "- **게임 이벤트**: `/topic/game/{roomId}` 구독\n" +
                        "- **자동 게임 시작**: 2명이 구독하면 WSEventListener가 자동으로 게임 시작\n\n" +
                        "### 👥 친구 초대 매칭 시 구독 경로 (수동 시작)\n" +
                        "- **1단계**: `/topic/room/{roomId}` 구독 (모든 이벤트 수신용)\n" +
                        "- **2단계**: `/app/room/join` 메시지 발행 (서버에 참가 알림)\n" +
                        "- **수동 게임 시작**: 방장이 `/app/room/{roomId}/start` 메시지 발행\n" +
                        "- **게임 진행**: 계속 `/topic/room/{roomId}` 사용 (퀴즈, 답안, 결과 등)\n\n" +
                        "## WebSocket 이벤트 타입\n\n" +
                        "### 📨 Client → Server (발행)\n" +
                        "**랜덤 매칭에서 사용:**\n" +
                        "1. **퀴즈 답안**: `/app/game/{roomId}/answer`\n\n" +
                        "**친구 초대 매칭에서 사용:**\n" +
                        "2. **방 참가**: `/app/room/join` (초대 코드 포함)\n" +
                        "3. **방 나가기**: `/app/room/{roomId}/leave`\n" +
                        "4. **게임 시작**: `/app/room/{roomId}/start` (방장만)\n" +
                        "5. **퀴즈 답안**: `/app/room/{roomId}/answer`\n" +
                        "6. **재연결**: `/app/room/{roomId}/reconnect`\n\n" +
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
                        "```\n\n" +
                        "**TIMER** - 문제 타이머 카운트다운 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"TIMER\",\n" +
                        "  \"remainingTime\": 10\n" +
                        "}\n" +
                        "```\n" +
                        "※ remainingTime은 10부터 1까지 1초마다 감소\n\n" +
                        "**TIME_UP** - 문제 시간 종료 시\n" +
                        "```json\n" +
                        "{\n" +
                        "  \"type\": \"TIME_UP\",\n" +
                        "  \"message\": \"시간이 종료되었습니다!\"\n" +
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

        Server prodServer = new Server()
                .url(prodServerUrl)
                .description("Production Server");

        Server devServer = new Server()
                .url(devServerUrl)
                .description("Development Server");

        return new OpenAPI()
                .info(info)
                .servers(List.of(prodServer, devServer))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    public SwaggerConfig(MappingJackson2HttpMessageConverter converter) {
        var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType("application", "octet-stream"));
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }
}
