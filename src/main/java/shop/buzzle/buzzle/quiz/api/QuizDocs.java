package shop.buzzle.buzzle.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResultResDto;
import java.util.List;

@Tag(name = "[퀴즈 API]", description = "퀴즈 관련 API")
public interface QuizDocs {

    @Operation(summary = "단일 퀴즈 생성", description = "단일 퀴즈를 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "퀴즈 생성 성공",
                            content = @Content(schema = @Schema(implementation = QuizResDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<QuizResDto> advice(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email,
            @Parameter(description = "카테고리 종류 (HISTORY, FOUR_IDIOMS, CAPITAL, SCIENCE, ALL)", required = true) QuizReqDto quizReqDto);

    @Operation(summary = "다중 퀴즈 생성", description = "다중 퀴즈를 생성합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "퀴즈 생성 성공",
                            content = @Content(schema = @Schema(implementation = QuizResListDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<QuizResListDto> generateMultipleQuizzes(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email,
            @Parameter(description = "카테고리 종류 (HISTORY, FOUR_IDIOMS, CAPITAL, SCIENCE, ALL)와 요청할 퀴즈의 개수", required = true) QuizSizeReqDto quizSizeReqDto);

    @Operation(summary = "퀴즈 답안 제출", description = "퀴즈 답안을 제출하고 결과를 저장합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "답안 제출 처리 완료",
                            content = @Content(schema = @Schema(implementation = Boolean.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<Boolean> submitAnswer(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email,
            @Parameter(description = "퀴즈 답안 정보", required = true) QuizAnswerReqDto quizAnswerReqDto);

    @Operation(summary = "오답노트 조회", description = "틀린 문제들을 조회합니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "오답노트 조회 완료",
                            content = @Content(schema = @Schema(implementation = QuizResultResDto.class))),
                    @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                    @ApiResponse(responseCode = "401", description = "인증 실패"),
                    @ApiResponse(responseCode = "500", description = "서버 오류")
            })
    RspTemplate<List<QuizResultResDto>> getIncorrectNotes(
            @Parameter(description = "로그인한 유저의 이메일(토큰에서 자동 추출)", hidden = true) String email);
}
