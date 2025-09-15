package shop.buzzle.buzzle.websocket.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import shop.buzzle.buzzle.websocket.api.dto.AnswerRequest;
import shop.buzzle.buzzle.websocket.api.dto.WebSocketResponse;

@Tag(name = "WebSocket Events", description = "웹소켓 이벤트 문서화 (실제 API가 아님 - STOMP 프로토콜 사용)")
@RestController
@RequestMapping("/docs/websocket")
public class WebSocketDocumentationController {

    @Operation(
        summary = "[WebSocket] 일반 메시지 전송",
        description = """
            **실제 엔드포인트**: STOMP `/app/room/{roomId}`
            **구독 주소**: `/topic/game/{roomId}`

            사용자가 채팅방에 일반 메시지를 보낼 때 사용합니다.
            """,
        responses = @ApiResponse(
            responseCode = "200",
            description = "메시지 전송 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WebSocketResponse.Send.class),
                examples = @ExampleObject(
                    value = """
                    {
                        "sender": "user@example.com",
                        "message": "안녕하세요!"
                    }
                    """
                )
            )
        )
    )
    @PostMapping("/room/{roomId}/message")
    public void sendChatMessage(
            @Parameter(description = "채팅방 ID") @PathVariable String roomId,
            @Parameter(description = "전송할 메시지") @RequestBody String message
    ) {
        // 문서화 용도로만 사용
    }

    @Operation(
        summary = "[WebSocket] 게임 메시지 전송",
        description = """
            **실제 엔드포인트**: STOMP `/app/game/{roomId}`
            **구독 주소**: `/topic/game/{roomId}`

            게임 진행 중 메시지를 전송할 때 사용합니다.
            """
    )
    @PostMapping("/game/{roomId}/message")
    public void sendGameMessage(
            @Parameter(description = "게임방 ID") @PathVariable String roomId,
            @Parameter(description = "전송할 게임 메시지") @RequestBody String message
    ) {
        // 문서화 용도로만 사용
    }

    @Operation(
        summary = "[WebSocket] 퀴즈 답안 제출",
        description = """
            **실제 엔드포인트**: STOMP `/app/game/{roomId}/answer`
            **구독 주소**: `/topic/game/{roomId}`

            사용자가 퀴즈 답안을 제출할 때 사용합니다.
            서버는 ANSWER_RESULT, LEADERBOARD 등의 이벤트로 응답합니다.
            """,
        responses = @ApiResponse(
            responseCode = "200",
            description = "답안 제출 성공 - 실제로는 WebSocket 이벤트로 응답",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "ANSWER_RESULT 이벤트",
                        value = """
                        {
                            "type": "ANSWER_RESULT",
                            "message": "사용자명님이 정답을 맞췄습니다.",
                            "correctIndex": 2,
                            "correct": true
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "LEADERBOARD 이벤트",
                        value = """
                        {
                            "type": "LEADERBOARD",
                            "currentLeader": "user@example.com",
                            "scores": {
                                "user1@example.com": 100,
                                "user2@example.com": 50
                            }
                        }
                        """
                    )
                }
            )
        )
    )
    @PostMapping("/game/{roomId}/answer")
    public void submitAnswer(
            @Parameter(description = "게임방 ID") @PathVariable String roomId,
            @Parameter(description = "제출할 답안") @RequestBody AnswerRequest answerRequest
    ) {
        // 문서화 용도로만 사용
    }
}