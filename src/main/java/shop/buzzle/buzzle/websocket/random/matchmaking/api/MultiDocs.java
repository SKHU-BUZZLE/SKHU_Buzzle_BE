package shop.buzzle.buzzle.websocket.random.matchmaking.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import shop.buzzle.buzzle.global.template.RspTemplate;

@Tag(name = "[1ㄷ1 맞짱 API]", description = "1ㄷ1 맞짱 관련 API")
public interface MultiDocs {

    @Operation(summary = "1ㄷ1 맞짱 버전 2", description = "1ㄷ1 맞짱 버전 2입니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "맞짱 매칭 중 / 완료",
                            content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<String> requestMatchV2(
            @Parameter(description = "1ㄷ1 맞짱 버전 2", hidden = true) String email);

    @Operation(summary = "1ㄷ1 맞짱 대기열 취소", description = "1ㄷ1 맞짱 대기열을 취소합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "대기열 취소 성공",
                            content = @Content(schema = @Schema(implementation = Void.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
            RspTemplate<Void> cancelMatchV2(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email);
}
