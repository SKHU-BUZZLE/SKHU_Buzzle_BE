package shop.buzzle.buzzle.multiroom.api.dto.response;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record InviteCodeValidationResDto(
        boolean valid,
        String message,
        RoomInfo roomInfo
) {
    public static InviteCodeValidationResDto valid(String roomId, String inviteCode, String hostName, int currentPlayers, int maxPlayers, QuizCategory category, int quizCount) {
        return new InviteCodeValidationResDto(
                true,
                "유효한 초대코드입니다.",
                new RoomInfo(roomId, inviteCode, hostName, currentPlayers, maxPlayers, category, quizCount, false)
        );
    }

    public static InviteCodeValidationResDto invalid(String reason) {
        return new InviteCodeValidationResDto(false, reason, null);
    }

    public record RoomInfo(
            String roomId,
            String inviteCode,
            String hostName,
            int currentPlayers,
            int maxPlayers,
            QuizCategory category,
            int quizCount,
            boolean gameStarted
    ) {}
}