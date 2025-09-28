package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchChannelsListRequest;
import com.chatting.backend.dto.websocket.outbound.FetchChannelsListResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

/**
 * [내 채널 목록 조회 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "FETCH_CHANNELS_LIST_REQUEST" 요청을 처리하는 클래스
 * - 즉, 내가 가입되어 있는(내가 참여자로 있는) 채널의 목록을 알기 위해 서버로 요청을 보내면,
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class FetchChannelsListRequestHandler implements BaseRequestHandler<FetchChannelsListRequest> {

    private final ChannelService channelService;
    private final ClientNotificationService clientNotificationService;

    /** [채팅방 목록 구하기]: 내가 현재 참여하고 있는 채팅방의 목록 보기(카카오톡 채팅목록)
     *
     * @param senderSession 채팅 목록 조회하려는 사람(요청자); 로그인한 사용자(=나)
     * @param request       FetchChannelsListRequest DTO
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchChannelsListRequest request) {
        // 요청자(나; 내 채팅 목록 조회자)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        List<Channel> channelList = channelService.getChannelsList(senderUserId);
        clientNotificationService.sendMessage(senderSession, senderUserId, new FetchChannelsListResponse(channelList));
    }
}
