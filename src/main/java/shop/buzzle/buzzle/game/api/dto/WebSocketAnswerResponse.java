package shop.buzzle.buzzle.game.api.dto;

public record WebSocketAnswerResponse(
        String type,
        String message,
        String correctAnswer,
        String userSelectedIndex,
        boolean correct,
        String userEmail,
        String userName
) {
    public static WebSocketAnswerResponse of(String userEmail, String username, boolean correct, String correctIndex, String userSelectedIndex) {
        String message = username + "님이 " + (correct ? "정답을 맞췄습니다!" : "틀렸습니다.");
        return new WebSocketAnswerResponse("ANSWER_RESULT", message, correctIndex, userSelectedIndex, correct, userEmail, username);
    }
}
