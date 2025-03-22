package shop.itcontest17.itcontest17.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import shop.itcontest17.itcontest17.global.annotation.CurrentUserEmail;
import shop.itcontest17.itcontest17.global.template.RspTemplate;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.request.QuizSizeReqDto;
import shop.itcontest17.itcontest17.quiz.api.dto.response.QuizDto;
import shop.itcontest17.itcontest17.quiz.application.QuizService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    // AI 응답
    @Operation(summary = "퀴즈 생성", description = "퀴즈를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping
    public RspTemplate<QuizDto> advice(@CurrentUserEmail String email
            , @RequestBody QuizReqDto quizReqDto) {
        return new RspTemplate<>(HttpStatus.OK, "퀴즈 생성 완료", quizService.askForAdvice(email, quizReqDto));
    }

    // 퀴즈 여러 개 생성
    @Operation(summary = "퀴즈 여러 개 생성", description = "퀴즈를 여러 개 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping("/multiple")
    public RspTemplate<List<QuizDto>> generateMultipleQuizzes(@CurrentUserEmail String email,
                                                              @RequestBody QuizSizeReqDto quizSizeReqDto) {
        List<QuizDto> quizList = quizService.askForAdvice(email, quizSizeReqDto);
        return new RspTemplate<>(HttpStatus.OK, "퀴즈 여러 개 생성 완료", quizList);
    }

    @Operation(summary = "정답 선택 시 true 반환", description = "정답 선택 시 true를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @PostMapping("/correct-answer")
    public RspTemplate<Boolean> chooseCorrectAnswer(@CurrentUserEmail String email) {
        return new RspTemplate<>(HttpStatus.OK, "정답 선택 완료", quizService.chooseTheCorrectAnswer(email));
    }

    @Operation(summary = "오답 선택 시 false 반환", description = "오답 선택 시 false를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "응답 생성을 성공했습니다"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청입니다"),
            @ApiResponse(responseCode = "401", description = "헤더 없음 or 토큰 불일치",
                    content = @Content(schema = @Schema(example = "INVALID_HEADER or INVALID_TOKEN")))
    })
    @GetMapping("/incorrect-answer")
    public RspTemplate<Boolean> chooseIncorrectAnswer() {
        return new RspTemplate<>(HttpStatus.OK, "오답 선택 완료", quizService.chooseTheIncorrectAnswer());
    }
}