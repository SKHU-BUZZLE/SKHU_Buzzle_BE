package shop.itcontest17.itcontest17.global.error.dto;

public record ErrorResponse(
        int statusCode,
        String message
) {
}