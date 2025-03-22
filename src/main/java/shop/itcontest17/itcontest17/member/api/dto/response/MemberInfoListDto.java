package shop.itcontest17.itcontest17.member.api.dto.response;

import java.util.List;
import lombok.Builder;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizResDto;

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
