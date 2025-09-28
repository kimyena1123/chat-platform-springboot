package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.Connection;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchConnectionsRequest;
import com.chatting.backend.dto.websocket.outbound.FetchConnectionsResponse;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * [나와 "ACCEPTED or PENDING" 상태인 사용자 조회 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "FETCH_CONNECTIONS_REQUEST" 요청을 처리하는 클래스
 * - 내가 나와 연결상태가 "수락된" 상태 또는 "대기" 상태인 사람들의 목록을 알기 위해 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class FetchConnectionsRequestHandler implements BaseRequestHandler<FetchConnectionsRequest> {

    private final UserConnectionService userConnectionService;
    private final ClientNotificationService clientNotificationService;

    /**
     * @param senderSession 로그인한 사용자(=나)
     * @param request       해당 request에는 status가 들어있음
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchConnectionsRequest request) {
        // 1) 요청자(나;사용자)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) 서비스에게 "해당 status에 해당하는 상대 목록"을 요청
        //    - userConnectionService.getUserByStatus(...) 는 List<User> 를 반환한다.
        //    - 반환된 User 도메인을 WebSocket 응답 DTO(Connection)로 매핑한다.
        List<Connection> connections = userConnectionService.getUserByStatus(senderUserId, request.getStatus())
                // User 도메인 -> Connection DTO 변환: 프론트가 기대하는 형태로 축약/포장
                .stream()
                .map(user -> new Connection(user.username(), request.getStatus()))
                // user.username() 은 User 도메인에서 username을 가져오는 메서드라고 가정
                // request.getStatus() 를 함께 넣는 이유는 프론트가 각 항목에 상태 표시를 원할 때 사용
                .toList();

        // 3) 변환된 연결 목록을 FetchConnectionsResponse로 감싸서 클라이언트(요청자)에게 전송
        clientNotificationService.sendMessage(senderSession, senderUserId, new FetchConnectionsResponse(connections));
    }
}
