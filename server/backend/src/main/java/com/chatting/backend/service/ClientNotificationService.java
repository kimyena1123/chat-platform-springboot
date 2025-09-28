package com.chatting.backend.service;

import com.chatting.backend.constant.MessageType;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.outbound.BaseMessage;
import com.chatting.backend.json.JsonUtil;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

/** [클라이언트(사용자)에게 메시지를 전송하는 서비스]
 * - WebSocketSessionManager + PushService를 추상화
 * - 핸들러는 이 서비스만 알면 되고, 푸시 서비스 존재 여부를 몰라도 됨
 */
@Slf4j
@Service
public class ClientNotificationService {

    private final WebSocketSessionManager webSocketSessionManager;
    private final PushService pushService;
    private final JsonUtil jsonUtil;

    public ClientNotificationService(WebSocketSessionManager webSocketSessionManager, PushService pushService, JsonUtil jsonUtil) {
        this.webSocketSessionManager = webSocketSessionManager;
        this.pushService = pushService;
        this.jsonUtil = jsonUtil;

        // 푸시 알림으로 전송할 수 있는 메시지 타입 등록
        pushService.registerPushMessageType(MessageType.INVITE_RESPONSE);
        pushService.registerPushMessageType(MessageType.ASK_INVITE);
        pushService.registerPushMessageType(MessageType.ACCEPT_RESPONSE);
        pushService.registerPushMessageType(MessageType.NOTIFY_ACCEPT);
        pushService.registerPushMessageType(MessageType.JOIN_RESPONSE);
        pushService.registerPushMessageType(MessageType.NOTIFY_JOIN);
        pushService.registerPushMessageType(MessageType.DISCONNECT_RESPONSE);
        pushService.registerPushMessageType(MessageType.REJECT_RESPONSE);
        pushService.registerPushMessageType(MessageType.CREATE_RESPONSE);
        pushService.registerPushMessageType(MessageType.QUIT_RESPONSE);
    }

    /** [세션 직접 지정해서 메시지 전송]
     *
     * 1) 나 자신에게 보낼 때: 세션을 이미 알고 있으니 (session, userId, message) 버전 사용.
     * 2) 다른 사람에게 보낼 때: 세션을 모를 수 있으니 (userId, message) 버전 사용.
     */

    // 1) 세션과 UserId 둘 다 넘기는 버전
    // 내가 지금 이 요청을 보낸 "나 자신"에게 바로 메시지를 보낼 때 사용
    // 이미 senderSession이 있으니, 굳이 세션을 조회할 필요가 없고 바로 사용하면 됨.
    public void sendMessage(WebSocketSession session, UserId userId, BaseMessage message){
        sendPayload(session, userId, message);
    }

    // 2) UserId만 넘기는 버전
    // 다른 사람(예: inviter)에게 메시지를 보낼 때 사용
    // 이 경우 senderSession을 직접 갖고 있지 않으니, 내부적으로 webSocketSessionManager.getSession(userId)를 호출해서 세션을 찾아낸다.
    public void sendMessage(UserId userId, BaseMessage message){
        sendPayload(webSocketSessionManager.getSession(userId), userId, message);
    }



    // 실제 메시지 전송 로직
    private void sendPayload(WebSocketSession session, UserId userId, BaseMessage message){
        Optional<String> json = jsonUtil.toJson(message);

        if (json.isEmpty()) { //json 파싱에 실패한 경우
           log.error("Send message failed. messageTYpe; {}", message.getType());
           return;
        }

        //json에 성공했다면 값 꺼내기(Optional이니까 값 꺼내기)
        String payload = json.get();

        try{
            if (session != null){
                webSocketSessionManager.sendMessage(session, payload);
            }else{
                // 오프라인이어도 푸시 알림으로 전송
                pushService.pushMessage(userId, message.getType(), payload);
            }
        }catch (Exception ex){
            // WebSocket 전송 실패 → 푸시 알림 대체
            pushService.pushMessage(userId, message.getType(), payload);
        }
    }

}
