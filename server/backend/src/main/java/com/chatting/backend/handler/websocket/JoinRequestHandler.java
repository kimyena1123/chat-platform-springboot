package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.JoinRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.JoinResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public class JoinRequestHandler implements BaseRequestHandler<JoinRequest> {

    private final ChannelService channelService;
    private final WebSocketSessionManager webSocketSessionManager;


    @Override
    public void handleRequest(WebSocketSession senderSession, JoinRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // ChannelService.join(...) 호출 결과를 받을 변수
        //    - Pair의 first: Optional<Channel> (성공 시 채널 정보)
        //    - Pair의 second: ResultType (성공/실패 사유 코드)
        Pair<Optional<Channel>, ResultType> result;

        //transaction으로 잡았기에 에러가 터질 수 있다. 해당 transaction이 있는건 에러 처리 해줘야 함
        try{
            result = channelService.join(request.getInviteCode(), senderUserId);
        }catch (Exception ex){
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.JOIN_REQUEST, ResultType.FAILED.getMessage()));
            return;
        }

        // 비즈니스 결과에 따라 분기 전송
        result.getFirst().ifPresentOrElse(
                // 요청자(본인)에게 JOIN_RESPONSE 전송
                channel -> webSocketSessionManager.sendMessage(senderSession, new JoinResponse(channel.channelId(), channel.title())),
                () -> webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.JOIN_REQUEST, result.getSecond().getMessage())));

    }
}
