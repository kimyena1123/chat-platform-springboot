package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.RejectRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.RejectResponse;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class RejectRequestHandler implements BaseRequestHandler<RejectRequest> {

    private final UserConnectionService userConnectionService;      // userConnectionService: reject()메서드를 사용하기 위해 필요함
    private final WebSocketSessionManager webSocketSessionManager;  // 메시지를 보내기 위해 SessionManager가 필요함

    /**
     * 여기서 요청을 보낸 사람 = 채팅 요청을 거절하는 사람 = rejector = senderSession
     *
     * @param senderSession 채팅 요청을 거절하는 사람
     * @param request 해당 request에는 채팅 초대를 보낸 사람의 username이 들어있음
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, RejectRequest request) {
        //1) 세션에서 userId 꺼내기 (세션에 userId가 저장되어 있어야 함)
        // - WebSocket 연결/핸드쉐이크 단계나 로그인 과정에서
        //   senderSession.getAttributes().put(IdKey.USER_ID.getValue(), userId)
        //   와 같은 식으로 세션에 UserId가 저장되어 있어야 한다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        //reject() 성공 반환값: true, 채팅 요청을 한 사람의 username
        //reject() 실패 반환값: false, 에러 메시지
        Pair<Boolean, String> result = userConnectionService.reject(senderUserId, request.getUsername());

        if(result.getFirst()) { // true이면(reject 성공이면)
            webSocketSessionManager.sendMessage(senderSession, new RejectResponse(request.getUsername(), UserConnectionStatus.REJECTED));
        }else{ // false이면(reject 실패이면)
            String errorMessage = result.getSecond();
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.REJECT_REQUEST, errorMessage));
        }

    }
}
