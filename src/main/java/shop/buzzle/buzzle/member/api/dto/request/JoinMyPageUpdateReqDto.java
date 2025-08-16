package shop.buzzle.buzzle.member.api.dto.request;

public record JoinMyPageUpdateReqDto(
        String introduction,
        String nationality,
        String school,
        String nickname
) {
}