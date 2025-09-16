package shop.buzzle.buzzle.multiroom.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomCreateReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomCreateResDto;

@Tag(name = "MultiRoom", description = "멀티플레이어 방 관리 API")
public interface MultiRoomDocs {

    @Operation(
            summary = "멀티플레이어 방 생성",
            description = """
                    새로운 멀티플레이어 게임 방을 생성합니다.

                    **제약사항:**
                    - 최대 플레이어 수: 2명~10명
                    - 퀴즈 개수: 3개~20개
                    - 인증된 사용자만 방 생성 가능

                    **퀴즈 카테고리:**
                    - ALL: 전체
                    - HISTORY: 역사
                    - SOCIETY: 사회
                    - SCIENCE: 과학
                    - CULTURE: 문화
                    - SPORTS: 스포츠
                    - NATURE: 자연
                    - MISC: 기타
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "방 생성 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MultiRoomCreateResDto.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답",
                                            value = """
                                                    {
                                                      "statusCode": 200,
                                                      "message": "방 생성",
                                                      "data": {
                                                        "inviteCode": "ABC123",
                                                        "maxPlayers": 4,
                                                        "category": "SCIENCE",
                                                        "quizCount": 10,
                                                        "hostName": "홍길동"
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (플레이어 수 또는 퀴즈 개수 범위 초과)",
                            content = @Content(
                                    examples = @ExampleObject(
                                            name = "에러 응답",
                                            value = """
                                                    {
                                                      "statusCode": 400,
                                                      "message": "최대 플레이어 수는 2명에서 10명 사이여야 합니다.",
                                                      "data": null
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증되지 않은 사용자",
                            content = @Content(
                                    examples = @ExampleObject(
                                            name = "인증 에러",
                                            value = """
                                                    {
                                                      "statusCode": 401,
                                                      "message": "인증이 필요합니다.",
                                                      "data": null
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<MultiRoomCreateResDto> createRoom(
            @Parameter(hidden = true) String email,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "방 생성 요청 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MultiRoomCreateReqDto.class),
                            examples = @ExampleObject(
                                    name = "요청 예시",
                                    value = """
                                            {
                                              "maxPlayers": 4,
                                              "category": "SCIENCE",
                                              "quizCount": 10
                                            }
                                            """
                            )
                    )
            )
            MultiRoomCreateReqDto multiRoomCreateReqDto
    );
}