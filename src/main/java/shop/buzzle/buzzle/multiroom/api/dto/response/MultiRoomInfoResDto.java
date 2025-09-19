package shop.buzzle.buzzle.multiroom.api.dto.response;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import java.util.List;

public record MultiRoomInfoResDto(
        String roomId,
        String inviteCode,
        String hostName,
        int maxPlayers,
        int currentPlayers,
        QuizCategory category,
        int quizCount,
        boolean gameStarted,
        boolean canStartGame,
        List<PlayerInfoDto> players
) {
    public record PlayerInfoDto(
            String email,
            String name,
            String picture,
            boolean isHost
    ) {}
}