package shop.buzzle.buzzle.multiroom.api.dto.response;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record MultiRoomCreateResDto(
        String inviteCode,
        String roomId,
        int maxPlayers,
        QuizCategory category,
        int quizCount,
        String hostName
) {
}