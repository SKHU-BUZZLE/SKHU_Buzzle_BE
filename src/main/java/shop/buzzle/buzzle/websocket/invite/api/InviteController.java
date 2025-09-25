package shop.buzzle.buzzle.websocket.invite.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import shop.buzzle.buzzle.global.annotation.CurrentUserEmail;
import shop.buzzle.buzzle.global.template.RspTemplate;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InvitedRoomCreateReqDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.request.InviteCodeValidationReqDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.invitedRoomCreateResDto;
import shop.buzzle.buzzle.websocket.invite.api.dto.response.InviteCodeValidationResDto;
import shop.buzzle.buzzle.websocket.invite.application.InviteRoomService;

@RestController
@RequestMapping("/api/multi-room")
@RequiredArgsConstructor
public class InviteController implements InvitedDocs {

    private final InviteRoomService inviteRoomService;

    @PostMapping
    public RspTemplate<invitedRoomCreateResDto> createRoom(@CurrentUserEmail String email, @RequestBody InvitedRoomCreateReqDto invitedRoomCreateReqDto) {
        invitedRoomCreateResDto room = inviteRoomService.createRoom(email, invitedRoomCreateReqDto);
        return new RspTemplate<>(HttpStatus.OK, "방 생성", room);
    }

    @PostMapping("/validate-invite")
    public RspTemplate<InviteCodeValidationResDto> validateInviteCode(@Valid @RequestBody InviteCodeValidationReqDto request) {
        InviteCodeValidationResDto result = inviteRoomService.validateInviteCode(request.inviteCode());

        if (result.valid()) {
            return new RspTemplate<>(HttpStatus.OK, "초대코드 검증 완료", result);
        } else {
            return new RspTemplate<>(HttpStatus.BAD_REQUEST, result.message(), result);
        }
    }
}