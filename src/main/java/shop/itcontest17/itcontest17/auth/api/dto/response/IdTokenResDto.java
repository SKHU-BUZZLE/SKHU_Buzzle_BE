package shop.itcontest17.itcontest17.auth.api.dto.response;

import com.fasterxml.jackson.databind.JsonNode;

public record IdTokenResDto(
        JsonNode idToken
) {
}
