package shop.buzzle.buzzle.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import shop.buzzle.buzzle.auth.api.dto.request.RefreshTokenReqDto;
import shop.buzzle.buzzle.auth.api.dto.request.TokenReqDto;
import shop.buzzle.buzzle.global.jwt.api.dto.TokenDto;
import shop.buzzle.buzzle.global.template.RspTemplate;

@Tag(name = "[인증 API]", description = "인증 관련 API")
public interface AuthDocs {

    @Operation(summary = "액세스 및 리프레시 토큰 발급", description = "회원 정보로부터 액세스 및 리프레시 토큰을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 발급 성공",
                            content = @Content(schema = @Schema(implementation = TokenDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<TokenDto> generateAccessAndRefreshToken(
            @Parameter(description = "OAuth2 제공자 (예: kakao, google)", required = true) String provider,
            @Parameter(description = "토큰 요청 데이터", required = true) TokenReqDto tokenReqDto);

    @Operation(summary = "액세스 토큰 재발급", description = "리프레시 토큰을 이용해 새로운 액세스 토큰을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "액세스 토큰 발급 성공",
                            content = @Content(schema = @Schema(implementation = TokenDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<TokenDto> generateAccessToken(
            @Parameter(description = "리프레시 토큰 요청 데이터", required = true) RefreshTokenReqDto refreshTokenReqDto);
}