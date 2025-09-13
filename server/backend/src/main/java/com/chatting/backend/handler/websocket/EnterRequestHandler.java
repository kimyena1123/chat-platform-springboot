package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.EnterRequest;
import com.chatting.backend.dto.websocket.outbound.EnterResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * [채팅방 입장 요청을 처리하는 핸들러]
 *
 * 카카오톡 예시:
 * - 사용자가 특정 채팅방(A방)을 클릭해서 "입장"할 때 서버에 EnterRequest를 보낸다고 생각
 * - 서버는 "정말 이 유저가 A방의 참여자가 맞는지 확인"하고, 맞다면 "임장 성고" 응답(방정보)와 함께 내부적으로 "현재 이 유저는 A방에 있음" 상태를 기록한다.
 */
@Component
@RequiredArgsConstructor
public class EnterRequestHandler implements BaseRequestHandler<EnterRequest> {

    private final ChannelService channelService;                    // 채널 관련 도메인 로직 (입장 가능 여부/상태 기록 등)
    private final WebSocketSessionManager webSocketSessionManager;  // 특정 세션으로 메시지 전송


    @Override
    public void handleRequest(WebSocketSession senderSession, EnterRequest request) {
        // 이 요청을 보낸 사용자(=지금 채팅방에 들어가려는 사람)의 userId를 WebSocket 세션에서 꺼낸다
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) 채널 입장 로직 수행
        //    - ChannelService.enter는 다음을 수행합니다:
        //      · 이 userId가 해당 channelId의 '참여자'인지 DB에서 확인 (미참여자면 NOT_JOINED)
        //      · 채널이 존재하는지 확인 (없으면 NOT_FOUND)
        //      · Redis에 "현재 활성 채널" 키를 TTL과 함께 기록 (앱 비정상 종료 시 자동정리)
        //    - 반환값:
        //      Pair<Optional<String>, ResultType> :
        //        · first   : Optional<title> (채널 제목)
        //        · second  : ResultType (SUCCESS / NOT_JOINED / NOT_FOUND / FAILED 등)
        Pair<Optional<String>, ResultType> result = channelService.enter(request.getChannelId(), senderUserId);

        // 성공/실패 분기
        result.getFirst().ifPresentOrElse(
                // (성공) title이 존재하면 → 입장 성공 응답
                title -> webSocketSessionManager.sendMessage(senderSession, new EnterResponse(request.getChannelId(), title)
                ), () -> {
                    // (실패) title이 없으면 → 실패 사유(ResultType)에 맞는 메시지로 ErrorResponse 전송
                    webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.ENTER_REQUEST, result.getSecond().getMessage()));
                });
    }
}
