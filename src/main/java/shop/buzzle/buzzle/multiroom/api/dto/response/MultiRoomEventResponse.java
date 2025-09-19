package shop.buzzle.buzzle.multiroom.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MultiRoomEventResponse(
        String type,
        String message,
        Object data
) {

    // 방 참가 성공
    public static MultiRoomEventResponse joinedRoom(MultiRoomInfoResDto roomInfo) {
        return new MultiRoomEventResponse("JOINED_ROOM", "방에 참가했습니다.", roomInfo);
    }

    // 플레이어 입장 알림
    public static MultiRoomEventResponse playerJoined(Member player) {
        var data = Map.of(
                "email", player.getEmail(),
                "name", player.getName(),
                "picture", player.getPicture() != null ? player.getPicture() : ""
        );
        return new MultiRoomEventResponse("PLAYER_JOINED", player.getName() + "님이 입장했습니다.", data);
    }

    // 플레이어 퇴장 알림
    public static MultiRoomEventResponse playerLeft(String playerName) {
        return new MultiRoomEventResponse("PLAYER_LEFT", playerName + "님이 퇴장했습니다.", Map.of("name", playerName));
    }

    // 문제 전송
    public static MultiRoomEventResponse question(String questionText, List<String> options, int questionIndex) {
        var data = Map.of(
                "question", questionText,
                "options", options,
                "questionIndex", questionIndex
        );
        return new MultiRoomEventResponse("QUESTION", "새 문제가 도착했습니다.", data);
    }

    // 에러 메시지
    public static MultiRoomEventResponse error(String errorMessage) {
        return new MultiRoomEventResponse("ERROR", errorMessage, null);
    }

    // 일반 메시지
    public static MultiRoomEventResponse message(String message) {
        return new MultiRoomEventResponse("MESSAGE", message, null);
    }

    // 게임 종료 (랭킹 포함)
    public static MultiRoomEventResponse gameEndWithRanking(GameEndResponseDto.GameEndData gameEndData) {
        String message;
        if (gameEndData.hasTie()) {
            message = "게임이 종료되었습니다! 동점자가 있습니다. 방이 해체됩니다.";
        } else {
            GameEndResponseDto.PlayerRanking winner = gameEndData.rankings().get(0);
            message = "게임이 종료되었습니다! 우승자: " + winner.name() + ". 방이 해체됩니다.";
        }

        return new MultiRoomEventResponse("GAME_END_RANKING", message, gameEndData);
    }
}