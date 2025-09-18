package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchChannelsListRequest;
import com.chatting.backend.dto.websocket.outbound.FetchChannelsListResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FetchChannelsListRequestHandler implements BaseRequestHandler<FetchChannelsListRequest> {

    private final ChannelService channelService;
    private final WebSocketSessionManager webSocketSessionManager;

    /** [채팅방 목록 구하기]: 내가 현재 참여하고 있는 채팅방의 목록 보기(카카오톡 채팅목록)
     *
     * @param senderSession 메시지를 보내려고 하는 사람의 세션(채팅을 보내는 자; 현재 이 플랫폼을 사용하는 "나"를 의미)
     * @param request
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchChannelsListRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        List<Channel> channelList = channelService.getChannelsList(senderUserId);
        webSocketSessionManager.sendMessage(senderSession, new FetchChannelsListResponse(channelList));
    }
}
