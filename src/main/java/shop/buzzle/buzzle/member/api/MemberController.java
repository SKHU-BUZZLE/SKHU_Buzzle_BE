package shop.buzzle.buzzle.member.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.member.api.dto.response.FullTopRankingResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberLifeResDto;
import shop.buzzle.buzzle.member.api.dto.response.RankingResDto;
import org.springframework.web.bind.annotation.RequestParam;
import shop.buzzle.buzzle.member.application.MemberProfileUpdaterService;
import shop.buzzle.buzzle.member.application.MemberService;
import shop.buzzle.buzzle.member.application.RedisRankingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/members")
public class MemberController implements MemberDocs {

    final private MemberService memberService;
    final private MemberProfileUpdaterService memberProfileUpdaterService;
    final private RedisRankingService redisRankingService;

    @GetMapping("/ranking")
    public RspTemplate<RankingResDto> getTop10MembersByStreak(
            @PageableDefault(size = 10) Pageable pageable,
            @CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "랭킹 조회 완료", memberService.getRankingWithPagination(pageable, email));
    }

    /**
     * 데이터를 Redis에 적재
     */
    @PostMapping("/redis/ranking/init")
    public RspTemplate<String> initRedisRanking() {
        redisRankingService.initializeRanking();
        return new RspTemplate<>(HttpStatus.OK, "Redis 랭킹 초기화 완료", "success");
    }

    /**
     * 상위 랭킹 + 내 랭킹 조회
     * -ZREVRANGE + ZREVRANK 사용
     */
    @GetMapping("/redis/ranking/top/full")
    public RspTemplate<FullTopRankingResDto> getTopRankingRedis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "Redis 랭킹 조회 완료",
                redisRankingService.getTopRankingFromRedis(page, size, email));
    }

    @GetMapping("/life")
    public RspTemplate<MemberLifeResDto> getMemberLife(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 life 조회 성공", memberService.getMemberLife(email));
    }

    @GetMapping("/my-page")
    public RspTemplate<MemberInfoResDto> getMyPage(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "회원 조회 성공", memberService.getMemberByEmail(email));
    }

//    @PostMapping("/image-update")
//    public RspTemplate<Void> updateMemberImage(@CurrentUserEmail String email,
//                                               @RequestBody AccessTokenRequest accessToken) {
//        return new RspTemplate<>(HttpStatus.OK, "회원 이미지 업데이트 성공",
//                memberProfileUpdaterService.updateProfileImage(email, accessToken));
//    }
}