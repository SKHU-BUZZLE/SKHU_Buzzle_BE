package shop.itcontest17.itcontest17.auth.api.dto.response;

public record IdTokenAndAccessTokenResponse(
        String access_token,
        String id_token
) {}

