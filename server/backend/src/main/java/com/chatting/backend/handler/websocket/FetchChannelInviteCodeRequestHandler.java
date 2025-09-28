package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchChannelInviteCodeRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.FetchChannelInviteCodeResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [채널 초대코드 요청 핸들러]
 *
 * - 클라이언트가 보낸 "FETCH_CHANNEL_INVITECODE_REQUEST" 요청을 처리하는 클래스
 * - 즉, 누군가 채널(채팅방)의 초대코드를 알고 싶을 때 서버로 요청하면,
 *   이 핸들러가 실행된다.
 *
 *   해당 채널의 초대코드를 알려면, 해당 채널의 참여자여야 한다.
 */
@Component
@RequiredArgsConstructor
public class FetchChannelInviteCodeRequestHandler implements BaseRequestHandler<FetchChannelInviteCodeRequest> {

    private final ChannelService channelService;
    private final ClientNotificationService clientNotificationService;


    /** [채팅방의 초대코드를 찾기]
     * : ex) 채팅방에 초대되어 있지 않은(채팅방에 참여되지 않은) 상대방에게 해당 채팅방의 초대코드를 알려주기 위함
     * @param senderSession 로그인한 사용자(=나)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchChannelInviteCodeRequest request) {
        // 1) 요청자(채널의 초대코드를 알기 위해 요청한 사람)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) 요청자(초대코드 요청자)가 그 해당 채널에 존재하는지 참여 여부 확인하기
        if (!channelService.isJoined(request.getChannelId(), senderUserId)) {
            clientNotificationService.sendMessage(
                    senderSession, senderUserId, new ErrorResponse(MessageType.FETCH_CHANNEL_INVITECODE_REQUEST, "Not joined the channel.")
            );

            return;
        }

        channelService.getInviteCode(request.getChannelId()).ifPresentOrElse(inviteCode ->
                // 요청자(채널 초대코드 요청자)에게 응답 보내기
                        clientNotificationService.sendMessage(senderSession, senderUserId, new FetchChannelInviteCodeResponse(request.getChannelId(), inviteCode)),

                // 실패했을 때
                () -> clientNotificationService.sendMessage(
                        senderSession, senderUserId,new ErrorResponse(MessageType.FETCH_CHANNEL_INVITECODE_REQUEST, "Fetch channel invite code failed.")
                )
        );
    }
}
