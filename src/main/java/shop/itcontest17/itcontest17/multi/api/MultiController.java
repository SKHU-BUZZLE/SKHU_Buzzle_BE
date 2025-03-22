package shop.itcontest17.itcontest17.multi.api;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.itcontest17.itcontest17.multi.application.MultiService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/match")
public class MultiController {

    private final MultiService multiService;

    @PostMapping
    public ResponseEntity<?> requestMatch(@RequestParam String userId) {
        String roomId = multiService.addToQueue(userId);

        return ResponseEntity.ok().body(Objects.requireNonNullElse(roomId, "대기 중입니다. 매칭 상대를 기다리고 있습니다."));
    }
}

