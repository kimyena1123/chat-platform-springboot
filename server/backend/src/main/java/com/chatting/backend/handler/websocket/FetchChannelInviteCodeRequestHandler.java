package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchChannelInviteCodeRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.FetchChannelInviteResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class FetchChannelInviteCodeRequestHandler implements BaseRequestHandler<FetchChannelInviteCodeRequest> {

    private final ChannelService channelService;
    private final WebSocketSessionManager webSocketSessionManager;


    /** [채팅방의 초대코드를 찾기]
     * : ex) 채팅방에 초대되어 있지 않은(채팅방에 참여되지 않은) 상대방에게 해당 채팅방의 초대코드를 알려주기 위함
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchChannelInviteCodeRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 요청자(초대코드 요청자)가 그 해당 채널에 존재하는지 참여 여부 확인하기
        if (!channelService.isJoined(request.getChannelId(), senderUserId)) {
            webSocketSessionManager.sendMessage(
                    senderSession, new ErrorResponse(MessageType.FETCH_CHANNEL_INVITECODE_REQUEST, "Not joined the channel.")
            );
            return;
        }

        channelService.getInviteCode(request.getChannelId()).ifPresentOrElse(inviteCode ->
                        webSocketSessionManager.sendMessage(senderSession, new FetchChannelInviteResponse(request.getChannelId(), inviteCode)),

                // 실패했을 때
                () -> webSocketSessionManager.sendMessage(
                        senderSession, new ErrorResponse(MessageType.FETCH_CHANNEL_INVITECODE_REQUEST, "Fetch channel invite code failed.")
                )
        );
    }
}
