package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.Constants;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.AcceptRequest;
import com.chatting.backend.dto.websocket.outbound.*;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AcceptRequestHandler implements BaseRequestHandler<AcceptRequest> {

    // 초대/연결 관련 비즈니스 로직을 담당하는 서비스
    private final UserConnectionService userConnectionService;

    // 특정 사용자 세션을 찾아 메시지를 보내기 위한 세션 매니저
    private final WebSocketSessionManager webSocketSessionManager;


    /**
     * 여기서 요청을 보낸 사람 = 수락자(Acceptor)이다. 즉, senderSession = acceptor라는 의미.
     *
     * Inviter(초대한 사람): "채탕하자"라고 먼저 초대 요청(InviteRequest)를 보낸 사람
     * Acceptor(수락한 사람 = 요청을 받은 사람) : 초대를 받은 뒤, "그래, 수락헐게"라고 수락 요청(AcceptRequest)를 보내는 사람
     *
     * [요청 흐름 순서]
     * 1. Inviter > 서버          : InviteRequest 전송. "이 username(또는 초대코드)인 사람과 연결하고 싶더"
     * 2. 서버 > Acceptor         : 초대 알림(InviteNotification) 전달
     * 3. Acceptor > 서버         : AcceptRequest 메시지 전송. "너가 보낸 요청, 내가 수락할게"
     * 4. 서버 > Inviter&Acceptor : 서로 연결 완료 메시지 전달(AcceptResponse, AcceptNotification). 이후부터 서로 채팅메시지를 주고 받을 수 있다
     *
     * @param senderSession 메시지를 보내려고 하는 사람의 세션(채팅을 보내는 자; 현재 이 플랫폼을 사용하는 "나"를 의미)
     * @param request       AcceptRequest: 클라이언트가 보낸 DTO. 해당 request에는 초대한 사람의 username이 들어있음
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, AcceptRequest request) {
        /**
         * 여기서 senderSession은 지금 서버에 "ACCEPT_REQUEST" 메시지를 보낸 클라이언트 세션이다.
         * 즉, 수락자(acceptor)가 서버로 보낸 요청을 처리하는 것이기에, 요청 보낸 사람 = 수락자이다.
         *
         * [주의]
         * InviterRequestHandler에서 "요청 보낸 사람"은 inviter
         * AcceptRequestHandler에서 "요청 보낸 사람"은 acceptor
         */

        // 1) 수락한 사람(acceptor)의 UserId를 webSocket 세션 attributes에서 꺼낸다.
        UserId acceptorUserId = (UserId) senderSession.getAttributes().get(Constants.USER_ID.getValue());

        // 2) 서비스 호출. accept()메서드의 반환값: inviterUserId, acceptorUsername
        //  - first: 성공시 초대한 사람(inviter)의 UserId
        //  - second: 성공시 수락자(acceptor)의 username
        Pair<Optional<UserId>, String> result = userConnectionService.accept(acceptorUserId, request.getUsername());

        result.getFirst().ifPresentOrElse(inviterUserId -> {
            // 성공 시: inviterUserId (초대한 사람의 아이디)가 존재
            String acceptorUseranme = result.getSecond();

            // 3-1) 수락자(요청 보낸 사람; acceptor)에게 수락 성공 응답 전송
            webSocketSessionManager.sendMessage(senderSession, new AcceptResponse(request.getUsername()));

            // 3-2) 초대한 사람(inviter)에게 수락 알림 전송
            webSocketSessionManager.sendMessage(webSocketSessionManager.getSession(inviterUserId), new AcceptNotification(acceptorUseranme));

        }, () -> {
            // 실패 시: first Optional이 비어있음 -> second는 에러 메시지
            String errorMessage = result.getSecond();

            // 수락 요청을 보낸 사람에게 에러 응답 전송
            // ErrorResponse에는 어떤 요청 타입에서 실패했는지(MessageType.ACCEPT_REQUEST)와 메시지를 전달
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.ACCEPT_REQUEST, errorMessage));
        });
    }
}
