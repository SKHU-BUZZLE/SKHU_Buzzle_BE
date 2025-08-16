package shop.buzzle.buzzle.member.api.dto.response;

import java.util.List;
import lombok.Builder;

@Builder
public record MemberInfoListDto(
        List<MemberInfoResDto> memberInfoResDtos
) {
    public static MemberInfoListDto from(List<MemberInfoResDto> memberInfoResDtos) {
        return MemberInfoListDto.builder()
                .memberInfoResDtos(memberInfoResDtos)
                .build();
    }
}
