package shop.buzzle.buzzle.auth.api.dto.response;

import groovy.transform.builder.Builder;

@Builder
public record AccessAndRefreshTokenResDto(
        String accessToken,
        String refreshToken
) {
}
