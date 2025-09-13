package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.Channel;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.CreateRequest;
import com.chatting.backend.dto.websocket.outbound.CreateResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.JoinNotification;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.UserService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * [1:1 Direct 채널 생성]
 *
 * 카카오톡 예시:
 * - 내가 친구 B와의 새로운 1:1 채팅방을 처음 개설할 때, 사용자(나)는 CreateRequest(title, participantUsername)을 서버로 보낸다
 * - 서버는 상대방 username이 실제로 있는지 확인 > 채널과 채널-사용자 매핑을 DB에 생성한 뒤 > 나에게는 생성 성공 응답, 상대방에게는 채팅방 초대되었다는 알림 보낸다.
 */
@Component
@RequiredArgsConstructor
public class CreateRequestHandler implements BaseRequestHandler<CreateRequest> {

    private final ChannelService channelService;                    // 채널 생성/참여자 매핑 저장 등 도메인 로직
    private final UserService userService;                          //사용자 정보를 조회하는데 사용(username -> userId 조회 목적)
    private final WebSocketSessionManager webSocketSessionManager;  // 특정 세션으로 메시지 전송

    @Override
    public void handleRequest(WebSocketSession senderSession, CreateRequest request) {
        // 1) 요청자(채널 생성자)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) request에 올라오는 username을 가지고서 userId를 알아내기
        Optional<UserId> userId = userService.getUserId(request.getParticipantUsername());

        //userId가 없는지 확인. 상대방이 존재하지 않으면 NOT_FOUND
        if (userId.isEmpty()) {
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.NOT_FOUND.getMessage()));
            return;
        }

        // 상대방의 userId
        UserId participantId = userId.get();

        // 채널 생성 트랜잭션 수행 (channel + channel_user 2건 저장)
        //    - ChannelService.create 내부:
        //        · title 유효성 검사 (비었거나 null이면 INVALID_ARGS)
        //        · channel 저장 후 생성된 channelId 획득
        //        · channel_user 에 (요청자, 상대방) 두 사용자 매핑 row 저장
        //        · 성공 시 Channel 도메인 객체(ChannelId, title, headCount)를 Optional 로 반환
        //      반환 타입: Pair<Optional<Channel>, ResultType>
        Pair<Optional<Channel>, ResultType> result;

        try{
            // 3) 채널 생성 트랜잭션 수행 (channel + channel_user 2건 작성)
            result = channelService.create(senderUserId, participantId, request.getTitle());
        }catch(Exception ex){
            // 내부 오류 → FAILED
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.FAILED.getMessage()));
            return;
        }

        // 4) 성공/실패 분기 후 응답/알림 송신
        result.getFirst().ifPresentOrElse(channel ->
        {
            // 성공: 요청자에게 CreateResponse
            webSocketSessionManager.sendMessage(senderSession, new CreateResponse(channel.channelId(), channel.title()));
            // 성공: 상대방에게 JoinNotification (상대 세션을 userId로 찾아 전송)
            webSocketSessionManager.sendMessage(webSocketSessionManager.getSession(participantId), new JoinNotification(channel.channelId(), channel.title()));

        }, () -> {
            // 실패: ResultType에 해당하는 메시지로 ErrorResponse
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, result.getSecond().getMessage())
            );
        });
    }
}
