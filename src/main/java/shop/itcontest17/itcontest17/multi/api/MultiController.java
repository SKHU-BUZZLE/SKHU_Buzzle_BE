package shop.itcontest17.itcontest17.multi.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.multi.api.dto.response.MultiResDto;
import shop.itcontest17.itcontest17.multi.api.dto.response.WinnerResDto;
import shop.itcontest17.itcontest17.multi.application.MultiService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/match")
public class MultiController implements MultiDocs{

    private final MultiService multiService;

    @PostMapping
    public CompletableFuture<MultiResDto> requestMatch(@CurrentUserEmail String email) {
        return multiService.addToQueue(email);
    }

    @PostMapping("/end")
    public RspTemplate<Boolean> winner(@RequestBody WinnerResDto winnerResDto) {
        return new RspTemplate<>(HttpStatus.OK, "승자 점수 처리 완료", multiService.winnerProcessing(winnerResDto.email()));
    }

    @PostMapping("/v2")
    public CompletableFuture<MultiResDto> requestMatchV2(@CurrentUserEmail String email) {
        return multiService.addToQueueV2(email);
    }


}

