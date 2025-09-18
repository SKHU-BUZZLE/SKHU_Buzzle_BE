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
    // 방 생성 성공
    public static MultiRoomEventResponse roomCreated(String roomId, String inviteCode, String hostName,
                                                   int maxPlayers, QuizCategory category, int quizCount) {
        var roomData = Map.of(
                "roomId", roomId,
                "inviteCode", inviteCode,
                "hostName", hostName,
                "maxPlayers", maxPlayers,
                "category", category.toString(),
                "quizCount", quizCount,
                "currentPlayers", 1,
                "canStart", false
        );
        return new MultiRoomEventResponse("ROOM_CREATED", "방이 생성되었습니다.", roomData);
    }

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

    // 게임 시작
    public static MultiRoomEventResponse gameStart(int totalQuestions, int countdownSeconds) {
        var data = Map.of(
                "totalQuestions", totalQuestions,
                "countdownSeconds", countdownSeconds
        );
        return new MultiRoomEventResponse("GAME_START", "게임이 시작됩니다!", data);
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

    // 답변 결과
    public static MultiRoomEventResponse answerResult(String playerName, boolean isCorrect, int correctIndex) {
        var data = Map.of(
                "playerName", playerName,
                "isCorrect", isCorrect,
                "correctIndex", correctIndex
        );
        String message = isCorrect ? playerName + "님이 정답을 맞혔습니다!" : playerName + "님이 오답을 선택했습니다.";
        return new MultiRoomEventResponse("ANSWER_RESULT", message, data);
    }

    // 리더보드 업데이트
    public static MultiRoomEventResponse leaderboard(String currentLeader, Map<String, Integer> scores) {
        var data = Map.of(
                "currentLeader", currentLeader,
                "scores", scores
        );
        return new MultiRoomEventResponse("LEADERBOARD", "점수가 업데이트되었습니다.", data);
    }

    // 게임 종료
    public static MultiRoomEventResponse gameEnd(String winner) {
        var data = Map.of("winner", winner);
        return new MultiRoomEventResponse("GAME_END", "게임이 종료되었습니다! 우승자: " + winner, data);
    }

    // 에러 메시지
    public static MultiRoomEventResponse error(String errorMessage) {
        return new MultiRoomEventResponse("ERROR", errorMessage, null);
    }

    // 로딩 메시지
    public static MultiRoomEventResponse loading(String loadingMessage) {
        return new MultiRoomEventResponse("LOADING", loadingMessage, null);
    }

    // 일반 메시지
    public static MultiRoomEventResponse message(String message) {
        return new MultiRoomEventResponse("MESSAGE", message, null);
    }

    public static MultiRoomEventResponse roomUpdated(MultiRoomInfoResDto roomInfo) {
        return new MultiRoomEventResponse("ROOM_UPDATED", "방 정보가 갱신되었습니다.", roomInfo);
    }
}