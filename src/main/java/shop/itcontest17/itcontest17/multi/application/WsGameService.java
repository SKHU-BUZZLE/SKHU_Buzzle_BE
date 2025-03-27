//package shop.itcontest17.itcontest17.multi.application;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
//import org.springframework.web.reactive.socket.server.WebSocketService;
//
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class WsGameService {
//
//    private final WebSocketService webSocketService;
//
//    public void startGame(String roomId, SimpMessageHeaderAccessor headerAccessor) {
//        // 유저 정보 (선택적)
//        String userId = webSocketService.getUserIdFromSession(headerAccessor);
//        String username = webSocketService.getUsernameFromSession(headerAccessor);
//
//        // 시스템 메시지: 게임 시작 알림
//        webSocketService.sendGameSystemMessage(roomId, "게임이 시작되었습니다!");
//
//        // 이벤트 메시지: 클라이언트가 처리할 수 있도록
//        webSocketService.sendGameEvent(roomId, "GAME_STARTED");
//
//        // 유저 개별 메시지: 예시용
//        webSocketService.sendGameSystemMessageToUser(userId, roomId, username + "님, 게임에 참여하셨습니다.");
//    }
//}
