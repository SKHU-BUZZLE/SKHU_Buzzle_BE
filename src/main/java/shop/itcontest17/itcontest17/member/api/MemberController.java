package shop.itcontest17.itcontest17.member.api;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.member.application.MemberService;
import shop.itcontest17.itcontest17.member.domain.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
public class MemberController {

    final private MemberService memberService;

    @GetMapping("/ranking")
    public RspTemplate<List<Member>> getTop10MembersByStreak() {
        return new RspTemplate<>(HttpStatus.OK, "상위 10등 조회 완료", memberService.getTop10MembersByStreak());
    }
}