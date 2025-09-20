package shop.buzzle.buzzle.quiz.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.QuizSizeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.request.RetryQuizAnswerReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResListDto;
import shop.buzzle.buzzle.quiz.api.dto.response.QuizResultResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.RetryQuizResDto;
import shop.buzzle.buzzle.quiz.api.dto.request.IncorrectQuizChallengeReqDto;
import shop.buzzle.buzzle.quiz.api.dto.response.IncorrectQuizChallengeResDto;
import shop.buzzle.buzzle.quiz.api.dto.response.IncorrectQuizChallengeResultResDto;

import java.util.List;

@Tag(name = "퀴즈 API", description = "퀴즈 문제 생성, 답안 제출, 오답노트 조회 및 재시도 등의 기능을 제공합니다.")
public interface QuizDocs {

    @Operation(
            summary = "다중 퀴즈 생성",
            description = """
                    요청한 카테고리와 개수만큼 퀴즈를 생성합니다.
                    카테고리 종류는 다음과 같습니다:
                    - HISTORY
                    - SCIENCE
                    - SOCIETY
                    - CULTURE
                    - SPORTS
                    - NATURE
                    - MISC
                    - ALL (랜덤)
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "퀴즈 생성 성공",
                            content = @Content(
                                    schema = @Schema(implementation = QuizResListDto.class),
                                    examples = @ExampleObject(
                                            name = "퀴즈 생성 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": {
                                                        "quizzes": [
                                                          {
                                                            "question": "물은 몇 도에서 끓나요?",
                                                            "option1": "80도",
                                                            "option2": "90도",
                                                            "option3": "100도",
                                                            "option4": "110도",
                                                            "answer": "3"
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<QuizResListDto> generateMultipleQuizzes(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,

            @RequestBody(
                    description = "퀴즈 카테고리 및 생성할 퀴즈 수",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = QuizSizeReqDto.class),
                            examples = @ExampleObject(
                                    name = "퀴즈 요청 예시",
                                    value = """
                                            {
                                              "category": "NATURE",
                                              "size": 3
                                            }
                                            """
                            )
                    )
            )
            QuizSizeReqDto quizSizeReqDto
    );

    @Operation(
            summary = "퀴즈 정답 제출",
            description = "사용자가 푼 퀴즈의 정답을 제출하고, 정답 여부를 저장합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "정답 제출 성공 여부 반환",
                            content = @Content(
                                    schema = @Schema(implementation = Boolean.class),
                                    examples = @ExampleObject(
                                            name = "정답 제출 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": true
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<Boolean> submitAnswer(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,

            @RequestBody(
                    description = "사용자의 퀴즈 정답 제출 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = QuizAnswerReqDto.class),
                            examples = @ExampleObject(
                                    name = "정답 제출 예시",
                                    value = """
                                            {
                                              "question": "고양이는 포유류인가요?",
                                              "option1": "아니요",
                                              "option2": "네",
                                              "option3": "아마도요",
                                              "option4": "모르겠어요",
                                              "correctAnswerNumber": "2",
                                              "userAnswerNumber": "2",
                                              "category": "NATURE"
                                            }
                                            """
                            )
                    )
            )
            QuizAnswerReqDto quizAnswerReqDto
    );

    @Operation(
            summary = "오답노트 조회",
            description = "회원이 이전에 틀린 문제들을 모두 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "오답노트 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = QuizResultResDto.class),
                                    examples = @ExampleObject(
                                            name = "오답노트 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": [
                                                        {
                                                          "id": 102,
                                                          "question": "고래는 물고기인가요?",
                                                          "option1": "네",
                                                          "option2": "아니요",
                                                          "option3": "상황에 따라 다름",
                                                          "option4": "모르겠어요",
                                                          "correctAnswerNumber": "2",
                                                          "userAnswerNumber": "1",
                                                          "category": "SCIENCE",
                                                          "isCorrect": false,
                                                          "createdAt": "2025-09-15T10:12:34"
                                                        }
                                                      ]
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<List<QuizResultResDto>> getIncorrectNotes(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email
    );

    @Operation(
            summary = "오답 문제 상세 조회",
            description = "특정 오답 문제의 상세 정보와 정답을 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "오답 문제 상세 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = RetryQuizResDto.class),
                                    examples = @ExampleObject(
                                            name = "오답 문제 상세 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "오답 문제 상세 조회 완료",
                                                      "data": {
                                                        "originalQuizId": 102,
                                                        "question": "고래는 물고기인가요?",
                                                        "option1": "네",
                                                        "option2": "아니요",
                                                        "option3": "상황에 따라 다름",
                                                        "option4": "모르겠어요",
                                                        "correctAnswer": "2",
                                                        "category": "SCIENCE"
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<RetryQuizResDto> getIncorrectQuizDetail(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,
            @Parameter(description = "조회할 오답 문제 ID", required = true)
            Long quizResultId
    );

    @Operation(
            summary = "오답 문제 재시도",
            description = "이전에 틀린 문제에 대해 다시 답안을 제출합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "재시도 결과 반환",
                            content = @Content(
                                    schema = @Schema(implementation = Boolean.class),
                                    examples = @ExampleObject(
                                            name = "재시도 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "재도전 성공! 정답입니다!",
                                                      "data": true
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<Boolean> retryIncorrectQuiz(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,
            @RequestBody(
                    description = "재시도 답안 제출 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RetryQuizAnswerReqDto.class),
                            examples = @ExampleObject(
                                    name = "재시도 답안 제출 예시",
                                    value = """
                                            {
                                              "originalQuizId": 102,
                                              "userAnswerNumber": "2"
                                            }
                                            """
                            )
                    )
            )
            RetryQuizAnswerReqDto retryQuizAnswerReqDto
    );

    @Operation(
            summary = "오답노트 퀴즈 삭제",
            description = "오답노트에서 특정 퀴즈 결과를 삭제합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "퀴즈 삭제 성공",
                            content = @Content(
                                    schema = @Schema(implementation = Void.class),
                                    examples = @ExampleObject(
                                            name = "퀴즈 삭제 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "오답노트에서 삭제 완료",
                                                      "data": null
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "존재하지 않는 퀴즈 결과"
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "본인의 퀴즈 결과가 아님"
                    )
            }
    )
    RspTemplate<Void> deleteIncorrectQuiz(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,
            @Parameter(description = "삭제할 퀴즈 결과 ID", required = true)
            Long quizResultId
    );

    @Operation(
            summary = "오답 재도전 문제 조회",
            description = "오답노트에서 최대 7문제를 랜덤으로 선택하여 재도전할 수 있는 문제들을 제공합니다. 각 문제당 제한시간은 10초입니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "오답 재도전 문제 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = IncorrectQuizChallengeResDto.class),
                                    examples = @ExampleObject(
                                            name = "오답 재도전 문제 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "오답 재도전 문제 조회 완료",
                                                      "data": {
                                                        "quizzes": [
                                                          {
                                                            "quizResultId": 102,
                                                            "question": "고래는 물고기인가요?",
                                                            "option1": "네",
                                                            "option2": "아니요",
                                                            "option3": "상황에 따라 다름",
                                                            "option4": "모르겠어요"
                                                          }
                                                        ],
                                                        "timeLimit": 10
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "재도전할 오답 문제가 없음"
                    )
            }
    )
    RspTemplate<IncorrectQuizChallengeResDto> getIncorrectQuizChallenge(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email
    );

    @Operation(
            summary = "오답 재도전 답안 제출",
            description = "오답 재도전 문제들의 답안을 제출합니다. 맞힌 문제는 오답노트에서 자동으로 제거됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "오답 재도전 완료",
                            content = @Content(
                                    schema = @Schema(implementation = IncorrectQuizChallengeResultResDto.class),
                                    examples = @ExampleObject(
                                            name = "오답 재도전 결과 응답 예시",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "오답 재도전 완료",
                                                      "data": {
                                                        "totalQuestions": 7,
                                                        "correctAnswers": 5,
                                                        "removedFromWrongNotes": 5,
                                                        "results": [
                                                          {
                                                            "quizResultId": 102,
                                                            "question": "고래는 물고기인가요?",
                                                            "userAnswer": "2",
                                                            "correctAnswer": "2",
                                                            "isCorrect": true,
                                                            "removedFromWrongNotes": true
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<IncorrectQuizChallengeResultResDto> submitIncorrectQuizChallenge(
            @Parameter(description = "로그인한 유저의 이메일 (토큰에서 자동 추출)", hidden = true)
            String email,
            @RequestBody(
                    description = "오답 재도전 답안 제출 요청",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = IncorrectQuizChallengeReqDto.class),
                            examples = @ExampleObject(
                                    name = "오답 재도전 답안 제출 예시",
                                    value = """
                                            {
                                              "answers": [
                                                {
                                                  "quizResultId": 102,
                                                  "userAnswerNumber": "2"
                                                },
                                                {
                                                  "quizResultId": 103,
                                                  "userAnswerNumber": "1"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            IncorrectQuizChallengeReqDto request
    );
}
