package shop.itcontest17.itcontest17.multi.application;

import java.util.LinkedList;
import java.util.Queue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultiService {
    private final Queue<String> waitingQueue = new LinkedList<>();

    public synchronized String matchUser(String userId) {
        if (!waitingQueue.isEmpty()) {
            // 큐에 다른 사람이 있으면 매칭 처리
            String opponent = waitingQueue.poll();
            String roomId = generateRoomId(userId, opponent);
            return roomId;
        }
        // 큐에 사람이 없으면 대기열에 추가
        waitingQueue.add(userId);
        return null;
    }

    private String generateRoomId(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }
}
