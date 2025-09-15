package shop.buzzle.buzzle.member.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.member.api.dto.request.AccessTokenRequest;
import shop.buzzle.buzzle.member.api.dto.response.MemberInfoResDto;
import shop.buzzle.buzzle.member.api.dto.response.MemberLifeResDto;
import shop.buzzle.buzzle.member.api.dto.response.RankingResDto;

@Tag(name = "Member", description = "멤버 관련 API")
public interface MemberDocs {

    @Operation(
            summary = "랭킹 조회 (페이지네이션)",
            description = "현재 로그인한 사용자를 포함한 전체 회원 랭킹을 페이지네이션 방식으로 조회합니다.",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY, example = "0", description = "페이지 번호"),
                    @Parameter(name = "size", in = ParameterIn.QUERY, example = "10", description = "페이지 당 항목 수"),
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "랭킹 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = RankingResDto.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": {
                                                        "pageInfo": {
                                                          "page": 0,
                                                          "size": 10,
                                                          "hasNext": true
                                                        },
                                                        "rankings": [
                                                          {
                                                            "picture": "https://example.com/profile.jpg",
                                                            "email": "alice@example.com",
                                                            "name": "앨리스",
                                                            "streak": 21,
                                                            "createAt": "2025-09-01T12:00:00",
                                                            "daysSinceCreation": 14,
                                                            "currentRanking": 1
                                                          }
                                                        ],
                                                        "totalMembers": 100,
                                                        "currentUser": {
                                                          "picture": "https://example.com/my.jpg",
                                                          "email": "me@example.com",
                                                          "name": "나",
                                                          "streak": 8,
                                                          "createAt": "2025-09-05T08:00:00",
                                                          "daysSinceCreation": 10,
                                                          "currentRanking": 42
                                                        }
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<RankingResDto> getTop10MembersByStreak(@Parameter(hidden = true) Pageable pageable,
                                                       @Parameter(hidden = true) String email);

    @Operation(
            summary = "회원 생명(Life) 조회",
            description = "현재 로그인한 회원의 life(생명) 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "생명 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MemberLifeResDto.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": {
                                                        "life": 3
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<MemberLifeResDto> getMemberLife(@Parameter(hidden = true) String email);

    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 사용자의 기본 프로필 정보를 조회합니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "회원 조회 성공",
                            content = @Content(
                                    schema = @Schema(implementation = MemberInfoResDto.class),
                                    examples = @ExampleObject(
                                            name = "성공 응답",
                                            value = """
                                                    {
                                                      "code": "200",
                                                      "message": "OK",
                                                      "data": {
                                                        "picture": "https://example.com/me.jpg",
                                                        "email": "me@example.com",
                                                        "name": "홍길동",
                                                        "streak": 15,
                                                        "createAt": "2025-08-30T14:35:00",
                                                        "daysSinceCreation": 17,
                                                        "currentRanking": 12
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    RspTemplate<MemberInfoResDto> getMyPage(@Parameter(hidden = true) String email);

//    @Operation(
//            summary = "카카오 프로필 이미지 업데이트",
//            description = "카카오 accessToken을 이용해 내 프로필 이미지를 업데이트합니다.",
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "업데이트 성공",
//                            content = @Content(
//                                    schema = @Schema(implementation = Void.class),
//                                    examples = @ExampleObject(
//                                            name = "성공 응답",
//                                            value = """
//                                                    {
//                                                      "code": "200",
//                                                      "message": "회원 이미지 업데이트 성공",
//                                                      "data": null
//                                                    }
//                                                    """
//                                    )
//                            )
//                    )
//            }
//    )
//    RspTemplate<Void> updateMemberImage(
//            @Parameter(
//                    name = "email",
//                    description = "로그인한 유저의 이메일 (토큰에서 추출됨)",
//                    required = true,
//                    example = "me@example.com"
//            )
//            String email,
//
//            @io.swagger.v3.oas.annotations.parameters.RequestBody(
//                    description = "카카오 AccessToken",
//                    required = true,
//                    content = @Content(
//                            schema = @Schema(implementation = AccessTokenRequest.class),
//                            examples = @ExampleObject(
//                                    name = "요청 예시",
//                                    value = """
//                                            {
//                                              "accessToken": "kakao-access-token-xyz"
//                                            }
//                                            """
//                            )
//                    )
//            )
//            AccessTokenRequest accessToken
//    );
}
