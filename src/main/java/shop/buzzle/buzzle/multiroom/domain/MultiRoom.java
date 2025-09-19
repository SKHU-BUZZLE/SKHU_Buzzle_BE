package shop.buzzle.buzzle.multiroom.domain;

import lombok.Getter;
import shop.buzzle.buzzle.quiz.domain.QuizCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Getter
public class MultiRoom {
    private final String roomId;
    private final String inviteCode;
    private String hostEmail;
    private final int maxPlayers;
    private final QuizCategory category;
    private final int quizCount;
    private final LocalDateTime createdAt;

    private final List<String> playerEmails = Collections.synchronizedList(new ArrayList<>());
    private boolean gameStarted = false;

    public MultiRoom(String roomId, String inviteCode, 
                    int maxPlayers, QuizCategory category, int quizCount) {
        this.roomId = roomId;
        this.inviteCode = inviteCode;
        this.maxPlayers = maxPlayers;
        this.category = category;
        this.quizCount = quizCount;
        this.createdAt = LocalDateTime.now();
    }

    public void setHost(String hostEmail) {
        if (this.hostEmail == null) {
            this.hostEmail = hostEmail;
        }
    }

    public boolean addPlayer(String playerEmail) {
        if (gameStarted) {
            return false;
        }
        if (playerEmails.size() >= maxPlayers) {
            return false;
        }
        if (playerEmails.contains(playerEmail)) {
            return false;
        }
        playerEmails.add(playerEmail);
        return true;
    }

    public void removePlayer(String playerEmail) {
        if (hostEmail.equals(playerEmail)) {
            return;
        }
        playerEmails.remove(playerEmail);
    }

    public boolean isHost(String playerEmail) {
        return hostEmail.equals(playerEmail);
    }

    public boolean canStartGame() {
        return !gameStarted && playerEmails.size() >= 2 && playerEmails.contains(hostEmail);
    }

    public void startGame() {
        if (!canStartGame()) {
            throw new IllegalStateException("게임을 시작할 수 없습니다.");
        }
        this.gameStarted = true;
    }

    public int getCurrentPlayerCount() {
        return playerEmails.size();
    }

    public List<String> getPlayerEmails() {
        return List.copyOf(playerEmails);
    }

    public boolean isFull() {
        return playerEmails.size() >= maxPlayers;
    }
}