package shop.itcontest17.itcontest17.global.jwt.api.dto;

import lombok.Builder;

@Builder
public record TokenDto(
        String accessToken,
        String refreshToken
) {
}