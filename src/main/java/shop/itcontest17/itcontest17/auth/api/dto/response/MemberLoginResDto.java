package shop.itcontest17.itcontest17.auth.api.dto.response;

import lombok.Builder;
import shop.itcontest17.itcontest17.member.domain.Member;

@Builder
public record MemberLoginResDto(
        Member findMember
) {
    public static MemberLoginResDto from(Member member) {
        return MemberLoginResDto.builder()
                .findMember(member)
                .build();
    }
}
