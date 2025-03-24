package shop.itcontest17.itcontest17.notification.application;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.itcontest17.itcontest17.member.domain.Member;
import shop.itcontest17.itcontest17.member.domain.repository.MemberRepository;
import shop.itcontest17.itcontest17.member.exception.MemberNotFoundException;
import shop.itcontest17.itcontest17.notification.domain.Notification;
import shop.itcontest17.itcontest17.notification.domain.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final SseEmitterManager sseEmitterManager;
    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;

    public SseEmitter connect(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);

        return sseEmitterManager.connect(member.getId());
    }

    @Transactional
    public void send(String email, String targetMemberEmail) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
        Member targetMember = memberRepository.findByEmail(targetMemberEmail)
                .orElseThrow(MemberNotFoundException::new);

        String message = member.getEmail() + "님과" + targetMember.getEmail() + "님이 매칭되었습니다.";

        notificationRepository.save(Notification.builder()
                .receiver(targetMember)
                .message(message)
                .build());

        sseEmitterManager.send(targetMember, message);
    }
}
