package shop.buzzle.buzzle.auth.api.dto.response;

public record IdTokenAndAccessTokenResponse(
        String access_token,
        String id_token
) {}

