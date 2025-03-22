package shop.itcontest17.itcontest17.member.api.dto.response;

import lombok.Builder;
import shop.itcontest17.itcontest17.member.domain.Member;

@Builder
public record MemberInfoResDto(
        String picture,
        String email,
        String name,
        Integer streak
) {
    public static MemberInfoResDto from(Member member) {
        return MemberInfoResDto.builder()
                .picture(member.getPicture())
                .email(member.getEmail())
                .name(member.getName())
                .streak(member.getStreak())
                .build();
    }
}
