package shop.itcontest17.itcontest17.member.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoListDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberInfoResDto;
import shop.itcontest17.itcontest17.member.api.dto.response.MemberLifeResDto;
import shop.itcontest17.itcontest17.member.application.MemberService;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
public class MemberController implements MemberDocs{

    final private MemberService memberService;

    @GetMapping("/ranking")
    public RspTemplate<MemberInfoListDto> getTop10MembersByStreak() {
        return new RspTemplate<>(HttpStatus.OK, "상위 10등 조회 완료", memberService.getTop10MembersByStreak());
    }

    @GetMapping("/life")
    public RspTemplate<MemberLifeResDto> getMemberLife(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 life 조회 성공", memberService.getMemberLife(email));
    }

    @GetMapping("/my-page")
    public RspTemplate<MemberInfoResDto> getMyPage(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 life 조회 성공", memberService.getMemberByEmail(email));
    }
}