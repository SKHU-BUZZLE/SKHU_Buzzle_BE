package shop.buzzle.buzzle.websocket.dto;

public record AnswerResponse(
        String type,
        String message,
        String correctAnswer,
        String userSelectedIndex,
        boolean correct,
        String userEmail,
        String userName
) {
    public static AnswerResponse of(String userEmail, String username, boolean correct, String correctIndex, String userSelectedIndex) {
        String message = username + "님이 " + (correct ? "정답을 맞췄습니다!" : "틀렸습니다.");
        return new AnswerResponse("ANSWER_RESULT", message, correctIndex, userSelectedIndex, correct, userEmail, username);
    }
}
