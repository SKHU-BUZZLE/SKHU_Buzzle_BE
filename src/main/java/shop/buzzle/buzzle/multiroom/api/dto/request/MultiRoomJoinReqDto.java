package shop.buzzle.buzzle.multiroom.api.dto.request;

public record MultiRoomJoinReqDto(
        String inviteCode
) {
    public MultiRoomJoinReqDto {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("초대 코드는 필수입니다.");
        }
    }
}