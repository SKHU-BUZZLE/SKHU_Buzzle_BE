package shop.itcontest17.itcontest17.auth.application;

import shop.itcontest17.itcontest17.auth.api.dto.response.IdTokenAndAccessTokenResponse;
import shop.itcontest17.itcontest17.auth.api.dto.response.IdTokenResDto;
import shop.itcontest17.itcontest17.auth.api.dto.response.UserInfo;

public interface AuthService {
    UserInfo getUserInfo(String authCode);

    String getProvider();

    IdTokenAndAccessTokenResponse getIdToken(String code);
}
