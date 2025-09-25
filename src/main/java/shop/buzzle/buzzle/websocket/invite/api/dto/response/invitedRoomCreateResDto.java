package shop.buzzle.buzzle.websocket.invite.api.dto.response;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record invitedRoomCreateResDto(
        String inviteCode,
        int maxPlayers,
        QuizCategory category,
        int quizCount,
        String hostName
) {
}
