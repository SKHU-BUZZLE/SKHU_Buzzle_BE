package shop.itcontest17.itcontest17.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import shop.itcontest17.itcontest17.auth.api.dto.request.TokenReqDto;
import shop.itcontest17.itcontest17.global.jwt.api.dto.TokenDto;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.domain.Member;

@Tag(name = "[멤버 API]", description = "멤버 관련 API")
public interface MemberDocs {

    @Operation(summary = "액세스 및 리프레시 토큰 발급", description = "회원 정보로부터 액세스 및 리프레시 토큰을 발급합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 발급 성공",
                            content = @Content(schema = @Schema(implementation = TokenDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<List<Member>> getTop10MembersByStreak();
}
