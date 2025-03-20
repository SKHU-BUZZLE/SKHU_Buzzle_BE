package shop.itcontest17.itcontest17.auth.api.dto.response;

public record UserInfo(
        String email,
        String name,
        String picture,
        String nickname
) {
}
