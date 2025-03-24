package shop.itcontest17.itcontest17.notification.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.notification.application.NotificationService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NotificationController implements NotificationDocs {

    private final NotificationService notificationService;

    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(
            @CurrentUserEmail String email) {
        return ResponseEntity.ok(notificationService.connect(email));
    }

    @PostMapping("/send/{targetMemberEmail}")
    public RspTemplate<Void> send(@CurrentUserEmail String email,
                                  @PathVariable String targetMemberEmail) {
        notificationService.send(email, targetMemberEmail);

        return new RspTemplate<>(HttpStatus.OK, "알림 전송 성공.");

    }
}
