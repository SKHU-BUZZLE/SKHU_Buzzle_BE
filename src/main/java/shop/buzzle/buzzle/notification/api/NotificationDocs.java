package shop.buzzle.buzzle.notification.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.buzzle.buzzle.global.template.RspTemplate;

@Tag(name = "[알림 API]", description = "알림 API")
public interface NotificationDocs {

    @Operation(summary = "SSE 연결", description = "SSE를 통해 실시간 알림을 받기 위한 연결.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    ResponseEntity<SseEmitter> connect(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email);

    @Operation(summary = "SSE로 알림 요청하기(프론트 test용)", description = "클라이언트의 요청으로 SSE를 통해 실시간 알림을 보낸다."
            + "대부분 약속한 상황(친구 관련, 좋아요 관련)에서는 서버에서 클라이언트에게 데이터를 자동으로 푸시해주지만,"
            + "테스트용으로 만들었다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "SSE 전송 성공"),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<Void> send(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email,
            @Parameter(description = "알림 받을 member 고유 email", required = true) String targetMemberEmail);
}