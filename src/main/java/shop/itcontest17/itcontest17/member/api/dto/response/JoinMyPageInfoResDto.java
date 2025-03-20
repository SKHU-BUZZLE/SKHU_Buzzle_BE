package shop.itcontest17.itcontest17.member.api.dto.response;

import lombok.Builder;
import shop.itcontest17.itcontest17.member.domain.Member;

@Builder
public record JoinMyPageInfoResDto(
        String introduction,
        String nickName

) {
    public static JoinMyPageInfoResDto from(Member member) {
        return JoinMyPageInfoResDto.builder()
                .introduction(member.getIntroduction())
                .nickName(member.getNickname())
                .build();
    }
}