package shop.itcontest17.itcontest17.game.api.dto;

public record WebSocketAnswerResponse(
        String type,
        String message,
        String correctAnswer,
        boolean correct
) {
    public static WebSocketAnswerResponse of(String username, boolean correct, String correctIndex) {
        String message = username + "님이 " + (correct ? "정답을 맞췄습니다!" : "틀렸습니다.");
        return new WebSocketAnswerResponse("ANSWER_RESULT", message, correctIndex, correct);
    }

    public static WebSocketAnswerResponse success(String username, String correctIndex) {
        return new WebSocketAnswerResponse("ANSWER_RESULT", username + "님이 정답을 맞췄습니다.", correctIndex, true);
    }

    public static WebSocketAnswerResponse fail(String username, String correctIndex) {
        return new WebSocketAnswerResponse("ANSWER_RESULT", username + "님이 틀렸습니다.", correctIndex, false);
    }
}
