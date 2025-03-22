package shop.itcontest17.itcontest17.multi.api;

import java.util.Objects;
import javax.print.MultiDocPrintService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shop.itcontest17.itcontest17.multi.application.MultiService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/match")
public class MultiController {

    private final MultiService multiService;

    @PostMapping("/find")
    public ResponseEntity<?> findMatch(@RequestBody MatchRequest request) {
        String roomId = multiService.matchUser(request.getUserId());
        return ResponseEntity.ok(new MatchResponse(Objects.requireNonNullElse(roomId, "Waiting for opponent...")));
    }

    // 매칭 요청 파라미터
    @Setter
    @Getter
    public static class MatchRequest {
        private String userId;

    }

    // 매칭 결과 응답 객체
    @Setter
    @Getter
    public static class MatchResponse {
        private String roomId;

        public MatchResponse(String roomId) {
            this.roomId = roomId;
        }

    }
}

