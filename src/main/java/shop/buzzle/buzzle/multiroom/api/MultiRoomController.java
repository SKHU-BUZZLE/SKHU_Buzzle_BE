package shop.buzzle.buzzle.multiroom.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.multiroom.api.dto.request.MultiRoomCreateReqDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomCreateResDto;
import shop.buzzle.buzzle.multiroom.api.dto.response.MultiRoomInfoResDto;
import shop.buzzle.buzzle.multiroom.application.MultiRoomService;

@RestController
@RequestMapping("/api/multi-room")
@RequiredArgsConstructor
public class MultiRoomController {

    private final MultiRoomService multiRoomService;

    @PostMapping
    public RspTemplate<MultiRoomCreateResDto> createRoom(@CurrentUserEmail String email, @RequestBody MultiRoomCreateReqDto multiRoomCreateReqDto) {
        MultiRoomCreateResDto room = multiRoomService.createRoom(email, multiRoomCreateReqDto);
        return new RspTemplate<>(HttpStatus.OK, "방 생성", room);
    }

    // 초대 코드로 방 정보 조회만 남김 (웹소켓 연결 전 미리보기용)
    @GetMapping("/invite/{inviteCode}")
    public RspTemplate<MultiRoomInfoResDto> getRoomInfoByInviteCode(
            @PathVariable String inviteCode
    ) {
        MultiRoomInfoResDto response = multiRoomService.getRoomInfoByInviteCode(inviteCode);
        return new RspTemplate<>(HttpStatus.OK, "초대 코드로 방 정보 조회 완료", response);
    }
}