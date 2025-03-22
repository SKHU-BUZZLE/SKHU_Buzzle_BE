package shop.itcontest17.itcontest17.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.ai.dto.AiResponseDto;

@RestController
@RequiredArgsConstructor
public class AiController {
    private final AiService service;

    // AI 응답
    @Operation(summary = "AI 응답 버전 몇인지 확인용", description = "AI에게 버전 몇인지 물어봅니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping("/ai")
    public ResponseEntity<AiResponseDto> advice() {
        return new ResponseEntity<>(service.askForAdvice(), HttpStatus.OK);
    }
}
