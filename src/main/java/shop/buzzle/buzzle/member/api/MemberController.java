package shop.buzzle.buzzle.member.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.member.api.dto.request.AccessTokenRequest;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoListDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberLifeResDto;
import shop.buzzle.buzzle.member.api.dto.response.RankingResDto;
import shop.buzzle.buzzle.member.application.MemberProfileUpdaterService;
import shop.buzzle.buzzle.member.application.MemberService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
public class MemberController implements MemberDocs {

    final private MemberService memberService;
    final private MemberProfileUpdaterService memberProfileUpdaterService;

    @GetMapping("/ranking")
    public RspTemplate<RankingResDto> getTop10MembersByStreak(
            @PageableDefault(size = 10) Pageable pageable,
            @CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "랭킹 조회 완료", memberService.getRankingWithPagination(pageable, email));
    }

    @GetMapping("/life")
    public RspTemplate<MemberLifeResDto> getMemberLife(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 life 조회 성공", memberService.getMemberLife(email));
    }

    @GetMapping("/my-page")
    public RspTemplate<MemberInfoResDto> getMyPage(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 조회 성공", memberService.getMemberByEmail(email));
    }

    @PostMapping("/image-update")
    public RspTemplate<Void> updateMemberImage(@CurrentUserEmail String email,
                                               @RequestBody AccessTokenRequest accessToken) {
        return new RspTemplate<>(HttpStatus.OK, "회원 이미지 업데이트 성공",
                memberProfileUpdaterService.updateProfileImage(email, accessToken));
    }
}