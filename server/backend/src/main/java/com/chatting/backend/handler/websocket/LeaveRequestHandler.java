package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.LeaveRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.LeaveResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class LeaveRequestHandler implements BaseRequestHandler<LeaveRequest> {

    private final ChannelService channelService;
    private final WebSocketSessionManager webSocketSessionManager;

    @Override
    public void handleRequest(WebSocketSession senderSession, LeaveRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        if(channelService.leave(senderUserId)){
            webSocketSessionManager.sendMessage(senderSession, new LeaveResponse());
        }else {
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.LEAVE_REQUEST, "Leave failed"));
        }
    }
}
