package shop.buzzle.buzzle.websocket.invite.api.dto.request;

public record InvitedRoomJoinReqDto(
        String inviteCode
) {
    public InvitedRoomJoinReqDto {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("초대 코드는 필수입니다.");
        }
    }
}