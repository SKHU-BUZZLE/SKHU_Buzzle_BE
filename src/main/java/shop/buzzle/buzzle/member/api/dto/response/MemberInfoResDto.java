package shop.buzzle.buzzle.member.api.dto.response;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Builder;
import shop.buzzle.buzzle.member.domain.Member;

@Builder
public record MemberInfoResDto(
        String picture,
        String email,
        String name,
        Integer streak,
        LocalDateTime createAt,
        Long daysSinceCreation,
        Integer currentRanking
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
