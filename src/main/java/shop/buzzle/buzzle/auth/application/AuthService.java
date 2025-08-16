package shop.buzzle.buzzle.auth.application;

import shop.buzzle.buzzle.auth.api.dto.response.IdTokenAndAccessTokenResponse;
import shop.buzzle.buzzle.auth.api.dto.response.UserInfo;

public interface AuthService {
    UserInfo getUserInfo(String authCode);

    String getProvider();

    IdTokenAndAccessTokenResponse getIdToken(String code);
}
