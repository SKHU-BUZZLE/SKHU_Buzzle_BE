package shop.itcontest17.itcontest17.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoListDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberLifeResDto;
import shop.itcontest17.itcontest17.member.domain.Member;

@Tag(name = "[멤버 API]", description = "멤버 관련 API")
public interface MemberDocs {

    @Operation(summary = "상위 10등 랭킹 조회", description = "상위 10등 랭킹 조회",
            responses = {
                    @ApiResponse(responseCode = "200", description = "랭킹 조회 성공",
                            content = @Content(schema = @Schema(implementation = MemberInfoListDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<MemberInfoListDto> getTop10MembersByStreak();

    @Operation(summary = "유저 life(생명) 조회", description = "유저 life(생명)을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "생명 조회 성공",
                            content = @Content(schema = @Schema(implementation = MemberLifeResDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<MemberLifeResDto> getMemberLife(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email);
}
