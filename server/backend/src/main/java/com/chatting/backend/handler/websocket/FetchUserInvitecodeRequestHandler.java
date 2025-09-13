package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.FetchUserInvitecodeRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.FetchUserInvitecodeResponse;
import com.chatting.backend.service.UserService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * FetUserInvitecodeRequestHandler
 *
 * 역할:
 *   - WebSocket으로 들어온 "내 초대코드 조회" 요청(FETCH_USER_INVITECODE_REQUEST)을 처리한다.
 *   - 요청을 보낸 WebSocket 세션의 소유자(=요청자)의 userId를 세션에서 확인하고,
 *     UserService를 통해 DB에서 저장된 inviteCode를 조회한 다음,
 *     조회 성공 시 FetchUserInvitecodeResponse로 해당 세션에 응답을 보낸다.
 *   - 조회 실패 시 ErrorResponse를 보내 클라이언트에 실패를 알린다.
 *
 * 설계 노트:
 *   - 이 핸들러는 '세션이 이미 인증/식별된 상태'라는 전제를 사용한다.
 *     따라서 요청 바디에 userId를 별도로 전달받지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FetchUserInvitecodeRequestHandler implements BaseRequestHandler<FetchUserInvitecodeRequest> {

    private final UserService userService;                          // UserService: DB에서 사용자 관련 정보(여기선 inviteCode)를 조회하는 서비스
    private final WebSocketSessionManager webSocketSessionManager;  // WebSocketSessionManager: WebSocket 세션을 찾고 메시지를 전송하는 유틸/관리자

    /**
     * 여기서 요청을 보낸 사람 = 사용자이다. 즉, senderSession = 자기자신을 의미.
     *
     * @param senderSession 요청을 보낸 사람의 WebSocketSession. (여기서는 '나 자신'의 세션)
     * @param request
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, FetchUserInvitecodeRequest request) {
        //1) 세션에서 userId 꺼내기 (세션에 userId가 저장되어 있어야 함)
        // - WebSocket 연결/핸드쉐이크 단계나 로그인 과정에서
        //   senderSession.getAttributes().put(IdKey.USER_ID.getValue(), userId)
        //   와 같은 식으로 세션에 UserId가 저장되어 있어야 한다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) UserService에 초대코드 조회 요청
        userService.getInviteCode(senderUserId).ifPresentOrElse(inviteCode ->
                // 성공: 본인 세션으로 초대코드가 담긴 응답 전송
                webSocketSessionManager.sendMessage(senderSession, new FetchUserInvitecodeResponse(inviteCode)),
                // 실패: 본인 세션으로 에러 응답 전송 (에러 메시지는 간단한 문구)
                () -> webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.FETCH_USER_INVITECODE_REQUEST, "Fetch user invite code failed.")));

    }
}
