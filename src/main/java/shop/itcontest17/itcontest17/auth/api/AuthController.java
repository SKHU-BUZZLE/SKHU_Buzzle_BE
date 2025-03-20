package shop.itcontest17.itcontest17.auth.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.auth.api.dto.request.RefreshTokenReqDto;
import shop.itcontest17.itcontest17.auth.api.dto.request.TokenReqDto;
import shop.itcontest17.itcontest17.auth.api.dto.response.IdTokenResDto;
import shop.itcontest17.itcontest17.auth.api.dto.response.MemberLoginResDto;
import shop.itcontest17.itcontest17.auth.api.dto.response.UserInfo;
import shop.itcontest17.itcontest17.auth.application.AuthMemberService;
import shop.itcontest17.itcontest17.auth.application.AuthService;
import shop.itcontest17.itcontest17.auth.application.AuthServiceFactory;
import shop.itcontest17.itcontest17.auth.application.TokenService;
import shop.itcontest17.itcontest17.global.jwt.api.dto.TokenDto;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.domain.SocialType;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceFactory authServiceFactory;
    private final AuthMemberService memberService;
    private final TokenService tokenService;

    @Operation(summary = "ID 토큰 반환", description = "ID 토큰을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ID 토큰 반환 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @GetMapping("oauth2/callback/{provider}")
    public IdTokenResDto callback(@PathVariable(name = "provider") String provider,
                                  @RequestParam(name = "code") String code) {
        AuthService authService = authServiceFactory.getAuthService(provider);
        return authService.getIdToken(code);
    }

    @Operation(summary = "회원가입과 동시에 access 토큰, refresh 토큰 반환", description = "access 토큰, refresh 토큰을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "access 토큰, refresh 토큰 반환 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping("/{provider}/token")
    public RspTemplate<TokenDto> generateAccessAndRefreshToken(
            @PathVariable(name = "provider") String provider,
            @RequestBody TokenReqDto tokenReqDto) {
        AuthService authService = authServiceFactory.getAuthService(provider);
        UserInfo userInfo = authService.getUserInfo(tokenReqDto.authCode());

        MemberLoginResDto getMemberDto = memberService.saveUserInfo(userInfo,
                SocialType.valueOf(provider.toUpperCase()));
        TokenDto getToken = tokenService.getToken(getMemberDto);

        return new RspTemplate<>(HttpStatus.OK, "토큰 발급", getToken);
    }

    @Operation(summary = "refresh 토큰으로 access 토큰 재발급", description = "refresh 토큰으로 access 토큰 재발급합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "refresh 토큰으로 access 토큰 재발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping("/token/access")
    public RspTemplate<TokenDto> generateAccessToken(@RequestBody RefreshTokenReqDto refreshTokenReqDto) {
        TokenDto getToken = tokenService.generateAccessToken(refreshTokenReqDto);

        return new RspTemplate<>(HttpStatus.OK, "액세스 토큰 발급", getToken);
    }
}
