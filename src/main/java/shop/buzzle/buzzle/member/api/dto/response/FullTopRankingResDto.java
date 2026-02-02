package shop.buzzle.buzzle.member.api.dto.response;

import java.util.List;
import lombok.Builder;
import shop.buzzle.buzzle.global.dto.PageInfoResDto;

@Builder
public record FullTopRankingResDto(
        PageInfoResDto pageInfo,
        List<FullRankingMemberResDto> rankings,
        long totalMembers,
        FullRankingMemberResDto currentUser  // 내 랭킹 정보
) {
    public static FullTopRankingResDto of(
            PageInfoResDto pageInfo,
            List<FullRankingMemberResDto> rankings,
            long totalMembers,
            FullRankingMemberResDto currentUser
    ) {
        return FullTopRankingResDto.builder()
                .pageInfo(pageInfo)
                .rankings(rankings)
                .totalMembers(totalMembers)
                .currentUser(currentUser)
                .build();
    }
}
