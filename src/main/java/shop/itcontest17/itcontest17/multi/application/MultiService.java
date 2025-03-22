package shop.itcontest17.itcontest17.multi.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor
public class MultiService {

    private final Queue<String> waitingQueue = new ConcurrentLinkedQueue<>();

    public String addToQueue(String userId) {
        waitingQueue.add(userId);

        // 두 명이 모이면 매칭 실행
        if (waitingQueue.size() >= 2) {
            String user1 = waitingQueue.poll();
            String user2 = waitingQueue.poll();
            String roomId = createRoomId(user1, user2);

            // 매칭된 사용자들에게 roomId 반환
            return roomId;
        }
        return null; // 매칭이 되지 않으면 null 반환
    }

    private String createRoomId(String user1, String user2) {
        return UUID.randomUUID().toString(); // 고유한 roomId 생성
    }
}