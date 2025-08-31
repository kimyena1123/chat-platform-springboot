package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.Constants;
import com.chatting.backend.constant.MessageType;
import com.chatting.backend.constant.UserConnectionStatus;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.InviteRequest;
import com.chatting.backend.dto.websocket.outbound.ErrorResponse;
import com.chatting.backend.dto.websocket.outbound.InviteNotification;
import com.chatting.backend.dto.websocket.outbound.InviteResponse;
import com.chatting.backend.service.UserConnectionService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/**
 * 초대 요청을 처리하는 WebSocket 핸들러
 *
 * 역할: 클라이언트(초대한 사람)가 보낸 "초대요청(InviteRequest)"를 받아 비즈니스 로직을 호출하고,
 *      그 결과를 초대한 사람과 초대받은 상대방에게 각각 알림 메시지로 전송한다.
 *
 * WebSocketSessionManager.sendMessage(session, message)의 session 매개변수는 "보낼 대상의 세션"이다.
 *  - 첫번째 전송은 senderSession : 초대한 본인에게 응답
 *  - 두번째 전송은 webSocketSessionManager.getSession(partnerUserId) ; 초대받은 상대방에게 알림
 *
 *  Pair<Optional<UserId>, String>의 읨;
 *      - first: 성공시 초대받은 사람의 UserId가 들어있는 Optional
 *      - second: 성공시 초대한 사람의 username(상대에게 보여줄 이름)
 */
@Component
@RequiredArgsConstructor
public class InviteRequestHandler implements BaseRequestHandler<InviteRequest> {

    // 초대/연결 관련 비즈니스 로직을 담당하는 서비스
    private final UserConnectionService userConnectionService;

    // 특정 사용자 세션을 찾아 메시지를 보내기 위한 세션 매니저
    private final WebSocketSessionManager webSocketSessionManager;


    /**
     * 초대 요청 처리의 진입점
     * 여기서 요청을 보낸 사람 = 초대자(invitor)이다. 즉, senderSession = invitorr라는 의미.
     *
     * @param senderSession 초대한 사람의 webSocketSession(메시지를 보낸 사람)
     * @param request       클라이언트가 보낸 초대 요청(초대코드 포함)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, InviteRequest request) {

        // 1) 초대한 사람(inviter)의 UserId를 webSocket 세션 attributes에서 꺼낸다.
        //      - 이 값은 handshake 인터셉터에서 미리 넣어둔 것이다.
        UserId inviterUserId = (UserId) senderSession.getAttributes().get(Constants.USER_ID.getValue());

        // 2) 비즈니스 로직 호출: 초대코드로 대상 사용자 찾고, 관계 상태를 PENDING으로 저장/갱신
        //      반환: Pair<Optional<UserId>, String>
        //          - first: Optional<partnerUserId> (성공시 존재)
        //          - second: inviterUsername 또는 처리 결과 메시지로 활용되는 문자열
        // invite()의 return값이 초대받은 사람의 userId와 초대한 사람의 username
        //  즉, result에는 초대받은 사람의 userId와 초대한 사람의 username이 담겨있다.
        Pair<Optional<UserId>, String> result = userConnectionService.invite(inviterUserId, request.getUserInviteCode());

        System.out.println("##########################################################################################");
        System.out.println(">>> Pair<Optional<UserId>, String> result보기 : " + result);

        // 3) 성공/실패 분기
        //      - getFirst(): Optional<UserId> >> 값이 있으면 성공(상대방을 찾았고 상태 저장 성공)
        //      - 값이 없으면 실패(잘못된 코드, 자기자신 초대, 이미 초대됨 등)
        result.getFirst().ifPresentOrElse(partnerUserId -> { //성공했을 때

            // 3-1) 초대한 사람의 username(inviterUsername). (상대방에게 보여줄 이름)이 두번째 값에 들어있다.
            String inviterUsername = result.getSecond();

            // 3-2) 상대방에게 메시지를 보내기. 초대한 사람(sender)에게 초대 요청 결과를 즉시 응답(PENDING)으로 돌려준다.
            //  요청을 보낸 사람 스스로에게 초대했다는 메세지 보내기
            webSocketSessionManager.sendMessage(senderSession, new InviteResponse(request.getUserInviteCode(), UserConnectionStatus.PENDING));

            // 3-3) 초대받은 상대방(partner)에게 "누가 당신을 초대했습니다" 알림을 보낸다.
            //  - 대상 세션을 userId로 찾는다
            //  - 메시지에는 "초대한 사람의 username"이 필요하므로 inviterUsername을 담는다.
            webSocketSessionManager.sendMessage(webSocketSessionManager.getSession(partnerUserId), new InviteNotification(inviterUsername)); //// 대상(상대방)의 세션, 알림 메시지(초대한 사람의 이름 포함)


        }, () -> {//실패했을 때
            String errormessage =result.getSecond();
            webSocketSessionManager.sendMessage(senderSession, new ErrorResponse(MessageType.INVITE_REQUEST, errormessage));
        });
    }
}
