package shop.buzzle.buzzle.websocket.dto;

public enum MessageType {
    // 시스템 메시지
    JOINED_ROOM,
    PLAYER_JOINED,
    PLAYER_LEFT,
    ERROR,
    MESSAGE,

    // 게임 진행 메시지
    GAME_START_NOTIFICATION,
    GAME_START,
    QUESTION,
    TIMER,
    TIME_UP,
    ANSWER_RESULT,
    LEADERBOARD,
    TIMER_STOP,
    LOADING,
    GAME_END_RANKING
}
