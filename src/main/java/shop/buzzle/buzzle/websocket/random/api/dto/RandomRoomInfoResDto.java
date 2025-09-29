package shop.buzzle.buzzle.websocket.random.api.dto;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;
import java.util.List;

public record RandomRoomInfoResDto(
        String roomId,
        QuizCategory category,
        int quizCount,
        int currentPlayers,
        List<PlayerInfoDto> players
) {
    public record PlayerInfoDto(
            String email,
            String name,
            String picture
    ) {}
}