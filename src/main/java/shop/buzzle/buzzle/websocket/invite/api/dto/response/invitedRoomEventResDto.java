package shop.buzzle.buzzle.websocket.invite.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import shop.buzzle.buzzle.member.domain.Member;
import shop.buzzle.buzzle.websocket.dto.MessageType;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record invitedRoomEventResDto(
        MessageType type,
        String message,
        Object data
) {

    // 방 참가 성공
    public static invitedRoomEventResDto joinedRoom(InvitedRoomInfoResDto roomInfo) {
        return new invitedRoomEventResDto(MessageType.JOINED_ROOM, "방에 참가했습니다.", roomInfo);
    }

    // 플레이어 입장 알림
    public static invitedRoomEventResDto playerJoined(Member player) {
        var data = Map.of(
                "email", player.getEmail(),
                "name", player.getName(),
                "picture", player.getPicture() != null ? player.getPicture() : ""
        );
        return new invitedRoomEventResDto(MessageType.PLAYER_JOINED, player.getName() + "님이 입장했습니다.", data);
    }

    // 플레이어 퇴장 알림
    public static invitedRoomEventResDto playerLeft(String playerName, String playerEmail) {
        var data = Map.of(
                "name", playerName,
                "email", playerEmail
        );
        return new invitedRoomEventResDto(MessageType.PLAYER_LEFT, playerName + "님이 퇴장했습니다.", data);
    }

    // 게임 시작 (전체 정보)
    public static invitedRoomEventResDto gameStart(int totalQuestions) {
        return new invitedRoomEventResDto(MessageType.GAME_START, "게임이 시작됩니다.", Map.of("totalQuestions", totalQuestions));
    }

    // 문제 전송
    public static invitedRoomEventResDto question(String questionText, List<String> options, int questionIndex) {
        var data = Map.of(
                "question", questionText,
                "options", options,
                "questionIndex", questionIndex
        );
        return new invitedRoomEventResDto(MessageType.QUESTION, "새 문제가 도착했습니다.", data);
    }

    // 리더보드 업데이트
    public static invitedRoomEventResDto leaderboard(String leaderName, String leaderEmail, Map<String, Integer> scores, Map<String, String> emailToName) {
        var data = Map.of(
                "currentLeader", leaderName,
                "currentLeaderEmail", leaderEmail,
                "scores", scores,
                "emailToName", emailToName
        );
        return new invitedRoomEventResDto(MessageType.LEADERBOARD, "리더보드가 업데이트되었습니다.", data);
    }

    // 에러 메시지
    public static invitedRoomEventResDto error(String errorMessage) {
        return new invitedRoomEventResDto(MessageType.ERROR, errorMessage, null);
    }

    // 일반 메시지
    public static invitedRoomEventResDto message(String message) {
        return new invitedRoomEventResDto(MessageType.MESSAGE, message, null);
    }

    // 게임 시작 알림
    public static invitedRoomEventResDto gameStartNotification() {
        return new invitedRoomEventResDto(MessageType.GAME_START_NOTIFICATION, "게임이 시작됩니다!", null);
    }

    // 게임 종료 (랭킹 포함)
    public static invitedRoomEventResDto gameEndWithRanking(GameEndResDto.GameEndData gameEndData) {
        String message;
        if (gameEndData.hasTie()) {
            message = "게임이 종료되었습니다! 동점자가 있습니다. 방이 해체됩니다.";
        } else {
            GameEndResDto.PlayerRanking winner = gameEndData.rankings().get(0);
            message = "게임이 종료되었습니다! 우승자: " + winner.name() + ". 방이 해체됩니다.";
        }

        return new invitedRoomEventResDto(MessageType.GAME_END_RANKING, message, gameEndData);
    }
}
