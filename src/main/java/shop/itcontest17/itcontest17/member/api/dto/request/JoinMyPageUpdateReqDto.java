package shop.itcontest17.itcontest17.member.api.dto.request;

public record JoinMyPageUpdateReqDto(
        String introduction,
        String nationality,
        String school,
        String nickname
) {
}