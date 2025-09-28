package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.QuitRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.QuitResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [채널 탈퇴 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "QUIT_REQUEST" 요청을 처리하는 클래스
 * - 즉, 내가 특정 채널을 탈퇴하고 싶어 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class QuitRequestHandler implements BaseRequestHandler<QuitRequest> {

    private final ChannelService channelService;
    private final ClientNotificationService clientNotificationService;

    @Override
    public void handleRequest(WebSocketSession senderSession, QuitRequest request) {
        // 요청자(채널 탈퇴자; 나)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 결과를 담을 변수
        ResultType result;

        //예외처리
        try{
            result = channelService.quit(request.getChannelId(), senderUserId);
        }catch (Exception ex){
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.QUIT_REQUEST, ResultType.FAILED.getMessage()));
            return;
        }

        if(result == ResultType.SUCCESS){
            clientNotificationService.sendMessage(senderSession, senderUserId, new QuitResponse(request.getChannelId()));
        }else{
            clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.QUIT_REQUEST, result.getMessage()));
        }

    }
}
