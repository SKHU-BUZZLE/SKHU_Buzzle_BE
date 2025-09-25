package shop.buzzle.buzzle.websocket.dto;

import java.util.List;

public record Question(
        String text, List<String> options, String answerIndex
) {

    public boolean isCorrectIndex(int index) {
        try {
            return index == Integer.parseInt(answerIndex) - 1;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}