package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchUserInvitecodeRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.FetchUserInvitecodeResponse;
import com.chatting.backend.service.ClientNotificationService;
import com.chatting.backend.service.UserService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * [내 초대코드 조회 요청 처리 핸들러]
 *
 * - 클라이언트가 보낸 "FETCH_USER_INVITECODE_REQUEST" 요청을 처리하는 클래스
 * - 즉, 내가 다른 사람에게 내 초대코드를 알려주기 위해 내 초대코드 조회를 서버로 요청을 보내면
 *   이 핸들러가 실행된다.
 */
@Component
@RequiredArgsConstructor
public class FetchUserInvitecodeRequestHandler implements BaseRequestHandler<FetchUserInvitecodeRequest> {

    private final UserService userService;
    private final ClientNotificationService clientNotificationService;

    /**
     * @param senderSession 로그인 한 사용자(= 나)
     * @param request       FetchUserInviteCodeRequest DTO
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchUserInvitecodeRequest request) {
        // 1) 요청자(나; 사용자)의 userId를 세션에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) UserService에 초대코드 조회 요청
        userService.getInviteCode(senderUserId).ifPresentOrElse(inviteCode ->
                // (성공) 나(사용자; 본인 세션)에게 응답 보내기 : "너의 초대코드 조회 성공했어"라는 응답 전송
                        clientNotificationService.sendMessage(senderSession, senderUserId, new FetchUserInvitecodeResponse(inviteCode)),
                // (실패) 본인 세션으로 에러 응답 전송 (에러 메시지는 간단한 문구)
                () -> clientNotificationService.sendMessage(senderSession, senderUserId, new ErrorResponse(MessageType.FETCH_USER_INVITECODE_REQUEST, "Fetch user invite code failed.")));

    }
}
