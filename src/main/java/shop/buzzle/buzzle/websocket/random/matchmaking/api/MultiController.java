package shop.buzzle.buzzle.websocket.random.matchmaking.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.websocket.random.matchmaking.application.MatchService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/match")
public class MultiController implements MultiDocs{

    private final MatchService matchService;

    @PostMapping("/v2")
    public RspTemplate<String> requestMatchV2(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, matchService.addToQueueV2(email));
    }

    @PostMapping("/cancel/v2")
    public RspTemplate<Void> cancelMatchV2(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "매칭 취소 완료", matchService.cancelMatch(email));
    }
}

