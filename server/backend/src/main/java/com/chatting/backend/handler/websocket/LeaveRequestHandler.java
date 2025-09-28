package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.LeaveRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.LeaveResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [채널 나가기 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "LEAVE_REQUEST" 요청을 처리하는 클래스
 * - 즉, 내가 특정 채널을 잠시 나가고 싶어서 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class LeaveRequestHandler implements BaseRequestHandler<LeaveRequest> {

    private final ChannelService channelService;
    private final ClientNotificationService clientNotificationService;

    @Override
    public void handleRequest(WebSocketSession senderSession, LeaveRequest request) {
        // 요청자(채널 나가려는 자; 나)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        if(channelService.leave(senderUserId)){
            clientNotificationService.sendMessage(senderSession, senderUserId, new LeaveResponse());
        }else {
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.LEAVE_REQUEST, "Leave failed"));
        }
    }
}
