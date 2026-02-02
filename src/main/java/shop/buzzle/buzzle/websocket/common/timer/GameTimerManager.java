package shop.buzzle.buzzle.websocket.common.timer;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import shop.buzzle.buzzle.websocket.common.event.domain.GameEvent.GameType;
import shop.buzzle.buzzle.websocket.common.event.domain.TimerExpiredEvent;
import shop.buzzle.buzzle.websocket.common.event.domain.TimerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class GameTimerManager {

    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final Map<String, List<ScheduledFuture<?>>> roomTimers = new ConcurrentHashMap<>();
    private final Map<String, Boolean> timerActiveFlags = new ConcurrentHashMap<>();

    /**
     * 게임 타이머 시작
     * @param roomId 방 ID
     * @param inviteCode 초대 코드 (초대방) 또는 roomId (랜덤매칭)
     * @param gameType 게임 타입 (INVITE 또는 RANDOM)
     * @param seconds 타이머 시간 (초)
     * @param questionIndex 현재 문제 인덱스
     */
    public void startTimer(String roomId, String inviteCode, GameType gameType, int seconds, int questionIndex) {
        // 기존 타이머 취소
        stopTimer(roomId);

        timerActiveFlags.put(roomId, true);
        List<ScheduledFuture<?>> tasks = new ArrayList<>();

        log.info("[TIMER_START] Room: {}, Duration: {}s, Question: {}", roomId, seconds, questionIndex);

        // 타이머 카운트다운 (매 초마다)
        for (int i = seconds; i > 0; i--) {
            final int remaining = i;
            ScheduledFuture<?> tickTask = scheduler.schedule(() -> {
                if (!isTimerActive(roomId)) {
                    return;
                }
                eventPublisher.publishEvent(new TimerTickEvent(roomId, inviteCode, gameType, remaining));
            }, seconds - i, TimeUnit.SECONDS);
            tasks.add(tickTask);
        }

        // 타이머 만료
        ScheduledFuture<?> expiredTask = scheduler.schedule(() -> {
            if (!isTimerActive(roomId)) {
                return;
            }
            log.info("[TIMER_EXPIRED] Room: {}, Question: {}", roomId, questionIndex);
            eventPublisher.publishEvent(new TimerExpiredEvent(roomId, inviteCode, gameType, questionIndex));
        }, seconds, TimeUnit.SECONDS);
        tasks.add(expiredTask);

        roomTimers.put(roomId, tasks);
    }

    /**
     * 게임 타이머 중지
     * @param roomId 방 ID
     */
    public void stopTimer(String roomId) {
        timerActiveFlags.put(roomId, false);

        List<ScheduledFuture<?>> tasks = roomTimers.remove(roomId);
        if (tasks != null) {
            int cancelledCount = 0;
            for (ScheduledFuture<?> task : tasks) {
                if (task.cancel(false)) {
                    cancelledCount++;
                }
            }
            log.info("[TIMER_STOP] Room: {}, Cancelled: {} tasks", roomId, cancelledCount);
        }
    }

    /**
     * 타이머가 활성화 상태인지 확인
     * @param roomId 방 ID
     * @return 활성화 여부
     */
    public boolean isTimerActive(String roomId) {
        return timerActiveFlags.getOrDefault(roomId, false);
    }

    /**
     * 방 정리 시 타이머 리소스 해제
     * @param roomId 방 ID
     */
    public void cleanupRoom(String roomId) {
        stopTimer(roomId);
        timerActiveFlags.remove(roomId);
        log.info("[TIMER_CLEANUP] Room: {} cleaned up", roomId);
    }

    @PreDestroy
    public void shutdown() {
        log.info("[TIMER_MANAGER] Shutting down scheduler...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
