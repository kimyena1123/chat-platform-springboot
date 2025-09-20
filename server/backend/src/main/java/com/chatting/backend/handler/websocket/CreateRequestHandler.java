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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * [1:1 Direct 채널 생성]
 * <p>
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

        // 2) request에 올라오는 여러 개의 username(1개일수도, 여러 개일수도 있다)으로 해당 userId 구하기 - 참여자들의 userId 구하기
        List<UserId> participantIds = userService.getUserIds(request.getParticipantUsernames());

        // userIds가 비어있는지 확인. 비어있다면 상대방이 존재하지 X -> NOT_FOUND
        if (participantIds.isEmpty()) {
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.NOT_FOUND.getMessage()));
            return;
        }


        // ChannelService에 있는 채널 생성 메서드의 결과를 저장할 변수 선언
        Pair<Optional<Channel>, ResultType> result;

        //transaction으로 잡았기에 에러가 터질 수 있다. 해당 transaction이 있는건 에러 처리 해줘야 함
        try {
            // 3) 채널 생성 트랜잭션 수행 (channel + channel_user 2건 작성)
            result = channelService.create(senderUserId, participantIds, request.getTitle());
        } catch (Exception ex) {
            // 내부 오류 → FAILED
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, ResultType.FAILED.getMessage()));
            return;
        }


        // Channel 정보가 비어있다면
        if (result.getFirst().isEmpty()) {
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.CREATE_REQUEST, result.getSecond().getMessage()));
        }


        //채널 정보를 담기
        Channel channel = result.getFirst().get();

        // 채팅방 개설을 한 후 (100명이라고 하자)
        // 채팅을 개설한 한명한테는 채팅방 개설을 한 것에 대한 응답(response)가 가야하고
        // 나머지 99명한테는 채팅방에 가입됐다는(채팅방이 생성되었다는) 알림을 보내야 한다.

        // - 채팅방 생성자(요청자)에게 보내는 응답
        webSocketSessionManager.sendMessage(senderSession, new CreateResponse(channel.channelId(), channel.title()));

        // - 채팅방 참여자들에게 보내는 알림
        //  CompletableFuture.runAsync(): 비동기 작업을 실행하기 위해 사용. 현재 실행 흐름(메인 스레드)을 막지 않고, 별도의 스레드에서 병렬로 실행되도록 한다.
        participantIds.forEach(participantId -> CompletableFuture.runAsync(() -> {

                //참여자들의 세션 구하기
                WebSocketSession participantSession = webSocketSessionManager.getSession(participantId);

                //참여자가 webSocket에 연결되어 있는지 확인(null이면 사용자가 아직 로그인하지 않았거나, 브라우저를 닫았거나, 연결이 끊어진 상태)이기에
                //이런 경우 메시지를 보낼 대상(세션)이 없으니 sendMessage를 호출할 수 없고 그냥 넘어가야 한다.
                if(participantSession != null){
                    //메시지 보내기: 참여자들의 세션, 보낼 알림(채팅방에 가입되었다는; 채팅방이 생성되었다는 알림)
                    //JoinNotification(channelId, title): 참여자들에게 어떤 채널의 어떤 채널명에 가입되었는지 알려주기 위함
                    webSocketSessionManager.sendMessage(participantSession, new JoinNotification(channel.channelId(), channel.title()));
                }
            }
        ));


    }
}
