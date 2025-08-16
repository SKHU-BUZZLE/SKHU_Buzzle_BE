package shop.buzzle.buzzle.global.error.dto;

public record ErrorResponse(
        int statusCode,
        String message
) {
}