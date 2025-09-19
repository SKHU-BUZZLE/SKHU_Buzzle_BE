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
import shop.buzzle.buzzle.multiroom.api.dto.request.InviteCodeValidationReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomCreateResDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.InviteCodeValidationResDto;

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

    @Operation(
            summary = "초대코드 검증",
            description = """
                    초대코드의 유효성을 검증하고 방 정보를 반환합니다.

                    **검증 항목:**
                    - 초대코드 존재 여부
                    - 방 존재 여부 (데이터 일관성 체크)
                    - 게임 진행 상태 (이미 시작된 게임은 참가 불가)
                    - 방 포화 상태 (정원이 찬 방은 참가 불가)

                    **사용 시나리오:**
                    1. 사용자가 초대코드 입력
                    2. 실시간으로 유효성 검증
                    3. 유효한 경우 방 정보 표시
                    4. WebSocket으로 방 참가 진행

                    **주의사항:**
                    - 인증 없이 사용 가능 (검증만 수행)
                    - 실제 방 참가는 WebSocket API 사용 필요
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "유효한 초대코드 - 검증 성공",
                            content = @Content(
                                    schema = @Schema(implementation = InviteCodeValidationResDto.class),
                                    examples = @ExampleObject(
                                            name = "검증 성공",
                                            value = """
                                                    {
                                                      "statusCode": 200,
                                                      "message": "초대코드 검증 완료",
                                                      "data": {
                                                        "valid": true,
                                                        "message": "유효한 초대코드입니다.",
                                                        "roomInfo": {
                                                          "roomId": "550e8400-e29b-41d4-a716-446655440000",
                                                          "inviteCode": "ABC123",
                                                          "hostName": "홍길동",
                                                          "currentPlayers": 2,
                                                          "maxPlayers": 4,
                                                          "category": "SCIENCE",
                                                          "quizCount": 10,
                                                          "gameStarted": false
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "무효한 초대코드 - 검증 실패",
                            content = @Content(
                                    schema = @Schema(implementation = InviteCodeValidationResDto.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "존재하지 않는 초대코드",
                                                    value = """
                                                            {
                                                              "statusCode": 400,
                                                              "message": "존재하지 않는 초대코드입니다.",
                                                              "data": {
                                                                "valid": false,
                                                                "message": "존재하지 않는 초대코드입니다.",
                                                                "roomInfo": null
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "이미 시작된 게임",
                                                    value = """
                                                            {
                                                              "statusCode": 400,
                                                              "message": "이미 게임이 시작된 방입니다.",
                                                              "data": {
                                                                "valid": false,
                                                                "message": "이미 게임이 시작된 방입니다.",
                                                                "roomInfo": null
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "방이 가득 참",
                                                    value = """
                                                            {
                                                              "statusCode": 400,
                                                              "message": "방이 가득 찼습니다.",
                                                              "data": {
                                                                "valid": false,
                                                                "message": "방이 가득 찼습니다.",
                                                                "roomInfo": null
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 형식 (Validation 에러)",
                            content = @Content(
                                    examples = @ExampleObject(
                                            name = "Validation 에러",
                                            value = """
                                                    {
                                                      "statusCode": 400,
                                                      "message": "초대코드는 6자리여야 합니다.",
                                                      "data": null
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<InviteCodeValidationResDto> validateInviteCode(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "검증할 초대코드 정보",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = InviteCodeValidationReqDto.class),
                            examples = @ExampleObject(
                                    name = "요청 예시",
                                    value = """
                                            {
                                              "inviteCode": "ABC123"
                                            }
                                            """
                            )
                    )
            )
            InviteCodeValidationReqDto request
    );
}