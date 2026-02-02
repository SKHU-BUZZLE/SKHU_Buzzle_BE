package shop.buzzle.buzzle.member.api.dto.response;

import lombok.Builder;
import shop.buzzle.buzzle.member.domain.Member;

@Builder
public record FullRankingMemberResDto(
        Long id,
        String nickname,
        String name,
        String picture,
        String email,
        Integer streak,
        Integer rank
) {
    public static FullRankingMemberResDto from(Member member, int rank) {
        return FullRankingMemberResDto.builder()
                .id(member.getId())
                .nickname(member.getNickname())
                .name(member.getName())
                .picture(member.getPicture())
                .email(member.getEmail())
                .streak(member.getStreak())
                .rank(rank)
                .build();
    }
}
