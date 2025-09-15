package shop.buzzle.buzzle.multi.api.dto.response;

public record MatchInfoDto(
        String roomId,
        String opponentName,
        String opponentProfileImage
) {
    public static MatchInfoDto of(String roomId, String opponentName, String opponentProfileImage) {
        return new MatchInfoDto(roomId, opponentName, opponentProfileImage);
    }
}