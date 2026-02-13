package shop.buzzle.buzzle.websocket.dto;

public record AnswerResponse(
        MessageType type,
        String message,
        String correctAnswer,
        String userSelectedIndex,
        boolean correct,
        String userEmail,
        String userName
) {
    public static AnswerResponse of(String userEmail, String username, boolean correct, String correctIndex, String userSelectedIndex) {
        String message = username + "님이 " + (correct ? "정답을 맞췄습니다!" : "틀렸습니다.");
        return new AnswerResponse(MessageType.ANSWER_RESULT, message, correctIndex, userSelectedIndex, correct, userEmail, username);
    }
}
