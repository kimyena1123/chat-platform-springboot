package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.RejectRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.RejectResponse;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [채팅 초대 거절 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "REJECT_REQUEST" 요청을 처리하는 클래스
 * - 즉, 누군가 나를 채팅방에 초대했을 때, 내가 "거절" 버튼을 눌러 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class RejectRequestHandler implements BaseRequestHandler<RejectRequest> {

    private final UserConnectionService userConnectionService;
    private final ClientNotificationService clientNotificationService;

    /**
     * @param senderSession 채팅 요청을 거절하는 사람
     * @param request 해당 request에는 채팅 초대를 보낸 사람의 username이 들어있음
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, RejectRequest request) {
        // // 1) 요청자(거절자)의 userId를 세션에서 꺼낸다.
        //      - WebSocket 연결/핸드쉐이크 단계나 로그인 과정에서
        //      senderSession.getAttributes().put(IdKey.USER_ID.getValue(), userId)
        //      와 같은 식으로 세션에 UserId가 저장되어 있어야 한다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        //reject() 성공 반환값: true, 채팅 요청을 한 사람의 username
        //reject() 실패 반환값: false, 에러 메시지
        Pair<Boolean, String> result = userConnectionService.reject(senderUserId, request.getUsername());

        if(result.getFirst()) { // true이면(reject 성공이면)
            clientNotificationService.sendMessage(senderSession, senderUserId, new RejectResponse(request.getUsername(), UserConnectionStatus.REJECTED));
        }else{ // false이면(reject 실패이면)
            String errorMessage = result.getSecond();
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.REJECT_REQUEST, errorMessage));
        }

    }
}
