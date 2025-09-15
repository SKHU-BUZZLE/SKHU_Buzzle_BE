package shop.buzzle.buzzle.member.api.dto.response;

import lombok.Builder;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;

import java.util.List;

@Builder
public record RankingResDto(
        PageInfoResDto pageInfo,
        List<MemberInfoResDto> rankings,
        long totalMembers,
        MemberInfoResDto currentUser
) {
    public static RankingResDto of(PageInfoResDto pageInfo, List<MemberInfoResDto> rankings,
                                   long totalMembers, MemberInfoResDto currentUser) {
        return RankingResDto.builder()
                .pageInfo(pageInfo)
                .rankings(rankings)
                .totalMembers(totalMembers)
                .currentUser(currentUser)
                .build();
    }
}