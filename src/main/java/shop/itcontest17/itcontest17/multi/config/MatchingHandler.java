//package shop.itcontest17.itcontest17.multi.config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.socket.WebSocketSession;
//
//import java.util.Queue;
//import java.util.concurrent.ConcurrentLinkedQueue;
//
//@Component
//@RequiredArgsConstructor
//public class MatchingHandler {
//
//    private final Queue<WebSocketSession> waitingQueue = new ConcurrentLinkedQueue<>();
//    private final SocketHandler socketHandler;
//
//    public void addToQueue(WebSocketSession session) {
//        WebSocketSession pairedSession = waitingQueue.poll();
//        if (pairedSession != null) {
//            // 매칭 성공 시 고유 roomId 생성 (UUID 사용 가능)
//            String roomId = "room_" + System.currentTimeMillis();
//            socketHandler.addSessionToRoom(roomId, session);
//            socketHandler.addSessionToRoom(roomId, pairedSession);
//
//            sendMessage(session, "Matching success! Room ID: " + roomId);
//            sendMessage(pairedSession, "Matching success! Room ID: " + roomId);
//        } else {
//            // 대기열에 추가
//            waitingQueue.add(session);
//            sendMessage(session, "Waiting for a match...");
//        }
//    }
//
//    private void sendMessage(WebSocketSession session, String message) {
//        try {
//            if (session.isOpen()) {
//                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
