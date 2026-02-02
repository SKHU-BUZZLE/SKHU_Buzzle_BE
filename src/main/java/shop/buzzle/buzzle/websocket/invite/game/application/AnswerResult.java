package shop.buzzle.buzzle.websocket.invite.game.application;

/**
 * 답변 처리 결과를 나타내는 Value Object.
 * 게임 세션에서 답변 처리 후 결과를 반환하여 side-effect 없이 테스트할 수 있도록 합니다.
 */
public record AnswerResult(
        boolean accepted,
        boolean correct,
        boolean wasFirst,
        String playerEmail,
        int selectedIndex,
        int correctIndex,
        String rejectionReason
) {
    /**
     * 정답 처리 결과 생성
     */
    public static AnswerResult correct(String playerEmail, int selectedIndex, int correctIndex, boolean wasFirst) {
        return new AnswerResult(true, true, wasFirst, playerEmail, selectedIndex, correctIndex, null);
    }

    /**
     * 오답 처리 결과 생성
     */
    public static AnswerResult incorrect(String playerEmail, int selectedIndex, int correctIndex) {
        return new AnswerResult(true, false, false, playerEmail, selectedIndex, correctIndex, null);
    }

    /**
     * 거부된 답변 결과 생성 (이미 종료된 게임, 중복 답변 등)
     */
    public static AnswerResult rejected(String reason) {
        return new AnswerResult(false, false, false, null, -1, -1, reason);
    }

    /**
     * 다음 문제로 진행해야 하는지 여부
     * @return 정답이면서 첫 번째 정답인 경우 true
     */
    public boolean shouldAdvance() {
        return accepted && correct && wasFirst;
    }

    /**
     * 점수가 증가해야 하는지 여부
     * @return 첫 번째 정답인 경우 true
     */
    public boolean shouldIncrementScore() {
        return accepted && correct && wasFirst;
    }

    /**
     * 라이프가 감소해야 하는지 여부
     * @return 오답인 경우 true
     */
    public boolean shouldDecrementLife() {
        return accepted && !correct;
    }
}
