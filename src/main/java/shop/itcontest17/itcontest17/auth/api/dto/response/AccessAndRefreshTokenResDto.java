package shop.itcontest17.itcontest17.auth.api.dto.response;

import groovy.transform.builder.Builder;

@Builder
public record AccessAndRefreshTokenResDto(
        String accessToken,
        String refreshToken
) {
}
