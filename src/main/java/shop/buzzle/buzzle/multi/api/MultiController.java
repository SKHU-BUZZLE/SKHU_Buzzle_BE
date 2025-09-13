package shop.buzzle.buzzle.multi.api;

import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.multi.api.dto.response.MultiResDto;
import shop.buzzle.buzzle.multi.api.dto.response.WinnerResDto;
import shop.buzzle.buzzle.multi.application.MultiService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/match")
public class MultiController implements MultiDocs{

    private final MultiService multiService;

//    @PostMapping
//    public CompletableFuture<MultiResDto> requestMatch(@CurrentUserEmail String email) {
//        return multiService.addToQueue(email);
//    }
//
//    @PostMapping("/end")
//    public RspTemplate<Boolean> winner(@RequestBody WinnerResDto winnerResDto) {
//        return new RspTemplate<>(HttpStatus.OK, "승자 점수 처리 완료", multiService.winnerProcessing(winnerResDto.email()));
//    }

    @PostMapping("/v2")
    public RspTemplate<String> requestMatchV2(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, multiService.addToQueueV2(email));
    }

    @PostMapping("/cancel/v2")
    public RspTemplate<Void> cancelMatchV2(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "매칭 취소 완료", multiService.cancelMatch(email));
    }

}

