package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.ResultType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.EnterRequest;
import com.chatting.backend.dto.websocket.outbound.EnterResponse;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.service.ChannelService;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * [채널(채팅방) 입장 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "ENTER_REQUEST" 요청을 처리하는 클래스
 * - 즉, 생성된 채널(채팅방에) 입장하기 위해 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class EnterRequestHandler implements BaseRequestHandler<EnterRequest> {

    private final ChannelService channelService;
    private final ClientNotificationService clientNotificationService;

    /**
     * @param senderSession 채널(채팅방) 입장하려는 자(요청자)
     * @param request       EnterRequest DTO
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, EnterRequest request) {
        // 1) 요청자(채널 입장하려는 자)의 userId를 세션에서 꺼낸다.
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
        result.getFirst().ifPresentOrElse( // title이 존재하면 → 입장 성공 응답
                // (성공) 입장하려는 자(요청자)에게 응답 보내기 : "입장 성공했어"라는 응답 전송
                title -> clientNotificationService.sendMessage(senderSession, senderUserId, new EnterResponse(request.getChannelId(), title)
                ), () -> { // (실패) title이 존재하지 않으면 → 실패 사유(ResultType)에 맞는 메시지로 ErrorResponse 전송
                    clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.ENTER_REQUEST, result.getSecond().getMessage()));
                });
    }
}
