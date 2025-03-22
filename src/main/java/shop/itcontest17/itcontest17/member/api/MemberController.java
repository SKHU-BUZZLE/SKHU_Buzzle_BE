package shop.itcontest17.itcontest17.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoListDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberLifeResDto;
import shop.itcontest17.itcontest17.member.application.MemberService;
import shop.itcontest17.itcontest17.member.domain.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
public class MemberController {

    final private MemberService memberService;

    @Operation(summary = "상위 10등 조회", description = "상위 10등 유저를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @GetMapping("/ranking")
    public RspTemplate<MemberInfoListDto> getTop10MembersByStreak() {
        return new RspTemplate<>(HttpStatus.OK, "상위 10등 조회 완료", memberService.getTop10MembersByStreak());
    }

    @Operation(summary = "회원 life 조회", description = "회원의 현재 life 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN"))),
            @ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없습니다",
                    content = @Content(schema = @Schema(example = "회원 정보를 찾을 수 없습니다.")))
    })
    @GetMapping("/life")
    public RspTemplate<MemberLifeResDto> getMemberLife(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 life 조회 성공", memberService.getMemberLife(email));
    }
}