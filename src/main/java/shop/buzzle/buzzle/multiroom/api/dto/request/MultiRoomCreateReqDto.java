package shop.buzzle.buzzle.multiroom.api.dto.request;

import shop.buzzle.buzzle.quiz.domain.QuizCategory;

public record MultiRoomCreateReqDto(
        int maxPlayers,
        QuizCategory category,
        int quizCount
) {
    public MultiRoomCreateReqDto {
        if (maxPlayers < 2 || maxPlayers > 10) {
            throw new IllegalArgumentException("최대 플레이어 수는 2명에서 10명 사이여야 합니다.");
        }
        if (quizCount < 3 || quizCount > 20) {
            throw new IllegalArgumentException("퀴즈 개수는 3개에서 20개 사이여야 합니다.");
        }
    }
}
