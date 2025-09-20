package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.QuitRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.QuitResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class QuitRequestHandler implements BaseRequestHandler<QuitRequest> {

    private final ChannelService channelService;
    private final WebSocketSessionManager webSocketSessionManager;

    @Override
    public void handleRequest(WebSocketSession senderSession, QuitRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        ResultType result;

        //예외처리
        try{
            result = channelService.quit(request.getChannelId(), senderUserId);
        }catch (Exception ex){
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.QUIT_REQUEST, ResultType.FAILED.getMessage()));
            return;
        }

        if(result == ResultType.SUCCESS){
            webSocketSessionManager.sendMessage(senderSession, new QuitResponse(request.getChannelId()));
        }else{
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.QUIT_REQUEST, result.getMessage()));
        }

    }
}
