package shop.buzzle.buzzle.member.application;

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoListDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberLifeResDto;
import shop.buzzle.buzzle.member.api.dto.response.RankingResDto;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.member.domain.repository.MemberRepository;
import shop.buzzle.buzzle.member.exception.MemberNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberLifeResDto getMemberLife(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        return new MemberLifeResDto(member.getLife());
    }

    public RankingResDto getRankingWithPagination(Pageable pageable, String currentUserEmail) {
        Page<Member> memberPage = memberRepository.findMembersByStreakWithPagination(pageable);

        List<MemberInfoResDto> rankings = memberPage.getContent()
                .stream()
                .map(member -> {
                    long daysSinceCreation = DAYS.between(
                            member.getCreatedAt().toLocalDate(),
                            LocalDate.now()
                    ) + 1;

                    Long rankingLong = memberRepository.findMemberRankingByStreak(member.getId());
                    int currentRanking = rankingLong != null ? rankingLong.intValue() : 1;

                    return MemberInfoResDto.builder()
                            .picture(member.getPicture())
                            .email(member.getEmail())
                            .name(member.getName())
                            .streak(member.getStreak())
                            .createAt(member.getCreatedAt())
                            .daysSinceCreation(daysSinceCreation)
                            .currentRanking(currentRanking)
                            .build();
                })
                .collect(Collectors.toList());

        PageInfoResDto pageInfo = PageInfoResDto.from(memberPage);

        long totalMembers = memberRepository.countAllMembers();

        MemberInfoResDto currentUser = getMemberByEmail(currentUserEmail);

        return RankingResDto.of(pageInfo, rankings, totalMembers, currentUser);
    }

    public MemberInfoResDto getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow(MemberNotFoundException::new);

        long daysSinceCreation = DAYS.between(
                member.getCreatedAt().toLocalDate(),
                LocalDate.now()
        ) + 1;

        Long rankingLong = memberRepository.findMemberRankingByStreak(member.getId());
        int currentRanking = rankingLong != null ? rankingLong.intValue() : 1;

        return MemberInfoResDto.builder()
                .picture(member.getPicture())
                .email(member.getEmail())
                .name(member.getName())
                .streak(member.getStreak())
                .createAt(member.getCreatedAt())
                .daysSinceCreation(daysSinceCreation)
                .currentRanking(currentRanking)
                .build();
    }
}
