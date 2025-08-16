package shop.buzzle.buzzle.member.api.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import shop.buzzle.buzzle.member.domain.Member;

@Builder
public record MemberInfoResDto(
        String picture,
        String email,
        String name,
        Integer streak,
        LocalDateTime createAt
) {
    public static MemberInfoResDto from(Member member) {
        return MemberInfoResDto.builder()
                .picture(member.getPicture())
                .email(member.getEmail())
                .name(member.getName())
                .streak(member.getStreak())
                .createAt(member.getCreatedAt())
                .build();
    }
}
