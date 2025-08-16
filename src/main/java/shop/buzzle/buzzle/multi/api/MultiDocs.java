package shop.buzzle.buzzle.multi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.CompletableFuture;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.multi.api.dto.response.MultiResDto;
import shop.buzzle.buzzle.multi.api.dto.response.WinnerResDto;

@Tag(name = "[1ㄷ1 맞짱 API]", description = "1ㄷ1 맞짱 관련 API")
public interface MultiDocs {

    @Operation(summary = "1ㄷ1 맞짱 대결 매칭 시작 / 이거는 안 쓰는 api임", description = "1ㄷ1 맞짱 대결 매칭 시작",
            responses = {
                    @ApiResponse(responseCode = "200", description = "맞짱 매칭 성공",
                            content = @Content(schema = @Schema(implementation = MultiResDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    CompletableFuture<MultiResDto> requestMatch(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email);

    @Operation(summary = "1ㄷ1 맞짱 승자 처리", description = "1ㄷ1 맞짱 승자를 처리합니다",
            responses = {
                    @ApiResponse(responseCode = "200", description = "승자 처리 성공",
                            content = @Content(schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<Boolean> winner(
            @Parameter(description = "승자 email", required = true)WinnerResDto winnerResDto);

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
