
import React, { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import './App.css';

const API_BASE_URL = 'http://localhost:8080';

function App() {
    const [jwt, setJwt] = useState('');
    const [stompClient, setStompClient] = useState(null);
    const [logs, setLogs] = useState([]);
    const [roomDetails, setRoomDetails] = useState(null);
    const [isHost, setIsHost] = useState(false);
    const [gameInfo, setGameInfo] = useState(null);
    const [currentQuestion, setCurrentQuestion] = useState(null);
    const [gameResult, setGameResult] = useState(null);

    // 입력용 상태
    const [inviteCodeInput, setInviteCodeInput] = useState('');
    const [maxPlayers, setMaxPlayers] = useState(2);
    const [category, setCategory] = useState('ALL');
    const [quizCount, setQuizCount] = useState(3);

    const stompClientRef = useRef(null);
    stompClientRef.current = stompClient;

    const log = (message) => {
        console.log(message);
        const timestamp = new Date().toLocaleTimeString();
        const formattedMessage = typeof message === 'object' ? JSON.stringify(message, null, 2) : message;
        setLogs(prevLogs => [`[${timestamp}] ${formattedMessage}`, ...prevLogs]);
    };

    useEffect(() => {
        return () => {
            if (stompClientRef.current) {
                log('Disconnecting...');
                stompClientRef.current.deactivate();
            }
        };
    }, []);

    const handleCreateRoom = async () => {
        if (!jwt) {
            log('JWT 토큰을 입력하세요.');
            return;
        }
        log('방 생성 요청...');
        try {
            const response = await fetch(`${API_BASE_URL}/api/multi-room`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${jwt}`
                },
                body: JSON.stringify({ maxPlayers, category, quizCount })
            });

            const data = await response.json();
            if (response.ok) {
                log('방 생성 성공:');
                log(data.data);
                setInviteCodeInput(data.data.inviteCode);
            } else {
                log(`방 생성 실패: ${data.message}`);
            }
        } catch (error) {
            log(`방 생성 에러: ${error.message}`);
        }
    };

    const handleJoinRoom = () => {
        if (!jwt) {
            log('JWT 토큰을 입력하세요.');
            return;
        }
        if (!inviteCodeInput) {
            log('초대 코드를 입력하세요.');
            return;
        }

        if (stompClient) {
            log('이미 연결되어 있습니다. 연결을 해제하고 다시 시도합니다.');
            stompClient.deactivate();
        }

        log(`연결 시도... (Token: ${jwt.substring(0, 10)}...)`);

        const client = new Client({
            webSocketFactory: () => new SockJS(`${API_BASE_URL}/chat?authorization=${jwt}`),
            onConnect: () => {
                log('✅ WebSocket 연결 성공!');
                setStompClient(client);

                // 개인 메시지 구독 (초기 방 정보 획득)
                client.subscribe('/user/queue/room', (message) => {
                    const body = JSON.parse(message.body);
                    log('📥 [개인 메시지 수신]:');
                    log(body);
                    if (body.type === 'JOINED_ROOM') {
                        const initialRoomDetails = body.data;
                        // 초기 플레이어 목록 설정
                        setRoomDetails(initialRoomDetails);
                        
                        // 방장 여부 확인
                        if (initialRoomDetails.players.length === 1 && initialRoomDetails.players[0].name === initialRoomDetails.hostName) {
                            setIsHost(true);
                            log('👑 당신이 방장입니다.');
                        }
                    }
                });

                // 방 전체 메시지 구독
                client.subscribe(`/topic/room/${inviteCodeInput}`, (message) => {
                    const body = JSON.parse(message.body);
                    log(`📥 [${inviteCodeInput} 방 메시지 수신]:`);
                    log(body);

                    switch (body.type) {
                        case 'PLAYER_JOINED':
                            setRoomDetails(prev => {
                                if (!prev) return null;
                                // 중복 입장 방지
                                if (prev.players.some(p => p.email === body.email)) return prev;

                                const newPlayer = { 
                                    email: body.email, 
                                    name: body.name, 
                                    picture: body.picture, 
                                    isHost: prev.players.length === 0 // 첫 플레이어는 방장
                                };

                                const newPlayers = [...prev.players, newPlayer];
                                return {
                                    ...prev,
                                    players: newPlayers,
                                    currentPlayerCount: newPlayers.length,
                                    canStartGame: newPlayers.length >= 2
                                };
                            });
                            break;
                        case 'PLAYER_LEFT':
                            setRoomDetails(prev => {
                                if (!prev) return null;
                                const newPlayers = prev.players.filter(p => p.email !== body.email);
                                return {
                                    ...prev,
                                    players: newPlayers,
                                    currentPlayerCount: newPlayers.length,
                                    canStartGame: newPlayers.length >= 2
                                };
                            });
                            break;
                        case 'MESSAGE':
                            alert(body.message); // 방 폭파 등 중요 메시지
                            handleDisconnect();
                            break;
                        case 'GAME_START':
                            setGameInfo({ totalQuestions: body.totalQuestions });
                            setCurrentQuestion(null);
                            setGameResult(null);
                            break;
                        case 'QUESTION':
                            setCurrentQuestion(body);
                            break;
                        case 'ANSWER_RESULT':
                             // UI에 정답 결과 표시 로직 (예: O/X 표시)
                            break;
                        case 'LEADERBOARD':
                            // UI에 리더보드 업데이트
                            break;
                        case 'GAME_END':
                            setGameResult(body);
                            setCurrentQuestion(null);
                            break;
                        default:
                            break;
                    }
                });

                log(`[${inviteCodeInput}] 방에 입장합니다...`);
                client.publish({
                    destination: '/app/room/join',
                    body: JSON.stringify({ inviteCode: inviteCodeInput })
                });
            },
            onStompError: (frame) => {
                log('Broker reported error: ' + frame.headers['message']);
                log('Additional details: ' + frame.body);
            },
            onWebSocketError: (event) => {
                log('WebSocket error: ' + event);
            },
            reconnectDelay: 5000,
        });

        client.activate();
    };

    const handleStartGame = () => {
        if (stompClient && roomDetails) {
            log('게임 시작 요청!');
            stompClient.publish({
                destination: `/app/room/${roomDetails.roomId}/start`,
                body: ''
            });
        }
    };
    
    const handleAnswerSubmit = (answerIndex) => {
        if (stompClient && roomDetails && currentQuestion) {
            log(`${answerIndex + 1}번 답변 제출`);
            stompClient.publish({
                destination: `/app/room/${roomDetails.roomId}/answer`,
                body: JSON.stringify({ 
                    questionIndex: currentQuestion.questionIndex,
                    index: answerIndex 
                })
            });
        }
    };

    const handleDisconnect = () => {
        if (stompClient) {
            stompClient.deactivate();
            setStompClient(null);
            setRoomDetails(null);
            setIsHost(false);
            setGameInfo(null);
            setCurrentQuestion(null);
            setGameResult(null);
            log('연결 해제');
        }
    };


    return (
        <div className="App">
            <div className="container">
                <h1>Buzzle Multi-Room Tester</h1>
                <div className="section">
                    <h2>1. 인증 (JWT)</h2>
                    <input
                        type="text"
                        placeholder="JWT Token"
                        value={jwt}
                        onChange={(e) => setJwt(e.target.value)}
                        className="jwt-input"
                    />
                </div>

                <hr />

                <div className="section">
                    <h2>2. 방 생성 (방장용)</h2>
                    <div>
                        <label>Max Players: </label>
                        <input type="number" value={maxPlayers} onChange={e => setMaxPlayers(parseInt(e.target.value))} min="2" max="10" />
                    </div>
                    <div>
                        <label>Category: </label>
                        <select value={category} onChange={e => setCategory(e.target.value)}>
                            <option value="ALL">ALL</option>
                            <option value="DATA_STRUCTURE">DATA_STRUCTURE</option>
                            <option value="OS">OS</option>
                            <option value="NETWORK">NETWORK</option>
                        </select>
                    </div>
                    <div>
                        <label>Quiz Count: </label>
                        <input type="number" value={quizCount} onChange={e => setQuizCount(parseInt(e.target.value))} min="3" max="20" />
                    </div>
                    <button onClick={handleCreateRoom}>방 생성</button>
                </div>

                <hr />

                <div className="section">
                    <h2>3. 방 참가 (모두)</h2>
                    <input
                        type="text"
                        placeholder="초대 코드"
                        value={inviteCodeInput}
                        onChange={(e) => setInviteCodeInput(e.target.value)}
                    />
                    <button onClick={handleJoinRoom} disabled={!!stompClient}>참가</button>
                    <button onClick={handleDisconnect} disabled={!stompClient}>나가기</button>
                </div>
                
                <hr />

                {roomDetails && (
                    <div className="section">
                        <h2>4. 방 정보</h2>
                        <p><strong>초대 코드:</strong> {roomDetails.inviteCode}</p>
                        <p><strong>방장:</strong> {roomDetails.hostName}</p>
                        <p><strong>인원:</strong> {roomDetails.currentPlayerCount} / {roomDetails.maxPlayers}</p>
                        <h3>플레이어 목록</h3>
                        <ul>
                            {roomDetails.players.map((p, i) => <li key={i}>{p.name} {p.isHost ? '👑' : ''}</li>)}
                        </ul>
                        {isHost && <button onClick={handleStartGame} disabled={!roomDetails.canStartGame}>게임 시작</button>}
                        {!roomDetails.canStartGame && isHost && <p><small>2명 이상이어야 시작할 수 있습니다.</small></p>}
                    </div>
                )}

                {gameInfo && (
                     <div className="section">
                        <h2>5. 게임 진행</h2>
                        {currentQuestion ? (
                            <div>
                                <h3>Question {currentQuestion.questionIndex + 1}/{gameInfo.totalQuestions}</h3>
                                <p>{currentQuestion.question}</p>
                                <ul>
                                    {currentQuestion.options.map((opt, index) => (
                                        <li key={index}>
                                            <button onClick={() => handleAnswerSubmit(index)}>
                                                {index + 1}. {opt}
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            </div>
                        ) : (
                            <p>다음 문제를 기다리는 중...</p>
                        )}
                        {gameResult && (
                            <div>
                                <h3>게임 종료!</h3>
                                <p>🏆 승자: {gameResult.winnerName}</p>
                            </div>
                        )}
                    </div>
                )}


            </div>
            <div className="logs-container">
                <h2>Logs</h2>
                <div className="logs">
                    {logs.map((logMsg, i) => <pre key={i}>{logMsg}</pre>)}
                </div>
            </div>
        </div>
    );
}

export default App;
