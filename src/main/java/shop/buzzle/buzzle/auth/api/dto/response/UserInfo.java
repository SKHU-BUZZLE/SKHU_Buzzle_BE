package shop.buzzle.buzzle.auth.api.dto.response;

public record UserInfo(
        String email,
        String name,
        String picture,
        String nickname
) {
}
