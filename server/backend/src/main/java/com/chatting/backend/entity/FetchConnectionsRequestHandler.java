package com.chatting.backend.entity;

import com.chatting.backend.constant.Constants;
import com.chatting.backend.dto.domain.Connection;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchConnectionsRequest;
import com.chatting.backend.dto.websocket.outbound.FetchConnectionsResponse;
import com.chatting.backend.handler.websocket.BaseRequestHandler;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;


@Component
@RequiredArgsConstructor
public class FetchConnectionsRequestHandler implements BaseRequestHandler<FetchConnectionsRequest> {

    private final UserConnectionService userConnectionService;
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * 여기서 요청을 보낸 사람 = 해당 요청을 보낸 사람 = 나 = 로그인한 사용자
     *
     *
     * @param senderSession 로그인한 사용자(=나)
     * @param request       해당 request에는 status가 들어있음
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchConnectionsRequest request) {
        //1) 세션에서 userId 꺼내기 (세션에 userId가 저장되어 있어야 함)
        // - WebSocket 연결/핸드쉐이크 단계나 로그인 과정에서
        //   senderSession.getAttributes().put(Constants.USER_ID.getValue(), userId)
        //   와 같은 식으로 세션에 UserId가 저장되어 있어야 한다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(Constants.USER_ID.getValue());

        // TODO: 실제 운영코드에서는 senderUserId가 null인지 체크해야 함(예: 인증이 안된 세션/만료된 세션 등에서의 방어 코드).

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
        webSocketSessionManager.sendMessage(senderSession, new FetchConnectionsResponse(connections));
    }
}
