package shop.itcontest17.itcontest17.notification.application;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.notification.exception.SendFailedException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60 * 24;

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(final Long memberId) {
        String emitterId = String.valueOf(memberId);

        // 새로운 SSE 연결 생성
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(emitterId, emitter);

        // SSE 연결 시 초기 메시지 전송
        registerEmitterCallbacks(emitter, emitterId);

        sendToClient(emitter, emitterId, "SSE 연결 완료: memberId ");
        return emitter;
    }

    private void registerEmitterCallbacks(SseEmitter emitter, String emitterId) {
        emitter.onCompletion(() -> {
            log.info("SSE 연결 종료: " + emitterId);
            removeEmitter(emitterId);
        });

        emitter.onTimeout(() -> {
            log.info("SSE 연결 시간 초과: " + emitterId);
            emitter.complete();
            removeEmitter(emitterId);
        });
    }

    public void send(Member targetMember, String message) {
        String emitterId = String.valueOf(targetMember.getId());
        SseEmitter emitter = emitters.get(emitterId);

        if (emitter != null) {
            sendToClient(emitter, emitterId, message);
        } else {
            log.warn("메시지를 보낼 수 없음: SSE 연결이 없음 (memberId: {})", emitterId);
        }
    }

    public void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .data(data));
        } catch (IOException exception) {
            removeEmitter(emitterId);
            throw new SendFailedException("전송 실패", exception);
        }
    }

    public void removeEmitter(String emitterId) {
        emitters.remove(emitterId);
    }
}
