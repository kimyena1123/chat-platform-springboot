package com.chatting.backend.handler.websocket;

import com.chatting.backend.constant.IdKey;
import com.chatting.backend.dto.domain.ChannelId;
import com.chatting.backend.dto.domain.UserId;
import com.chatting.backend.dto.websocket.inbound.WriteMessage;
import com.chatting.backend.dto.websocket.outbound.MessageNotification;
import com.chatting.backend.entity.MessageEntity;
import com.chatting.backend.repository.MessageRepository;
import com.chatting.backend.service.MessageService;
import com.chatting.backend.service.UserService;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/** [클라이언트가 WebSocket으로 보낸 "채팅 메시지 전송" 요청(WriteMessage)을 처리하는 핸들러]
 * - 1) 사용자가 채팅방에서 보낸 메시지를 서버가 받아서,
 * - 2) 메시지를 DB에 저장하고
 * - 3) 같은 채널을 보고 있는 다른 참여자들에게만(내가 있는 채널에 상대방도 활동중인 참여자) 실시간으로 전달하도록 연결.
 *
 */
@Component
@RequiredArgsConstructor
public class WriteMessageHandler implements BaseRequestHandler<WriteMessage> {

    private final UserService userService;
    private final MessageService messageService;
    private final WebSocketSessionManager webSocketSessionManager;

    /**
     * [상대방에게 메시지를 전달하는 역할 수행]\
     * : 어떤 사용자(나)가 어느 채널(channelId)에 어떤 내용(content)를 보낼건지 처리.
     *
     * @param senderSession 메시지를 보내는 사람의 WebSocket 세션
     * @param request       클라이언트가 보낸 메시지 DTO(channelId, content가 담겨 있다)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, WriteMessage request) {
        // 1) 이 요청을 보낸 사용자(= 메시지 발신자)의 userId를 WebSocket 세션 attributes에서 꺼낸다.
        UserId senderUserId = (UserId) senderSession.getAttributes().get(IdKey.USER_ID.getValue());

        // 2) 요청 payload에서 채널 ID와 전송할 content 꺼내기
        ChannelId channelId = request.getChannelId();
        String content = request.getContent();

        // 3) 메시지를 보내는 사람의 username 조회(상대에게 "누가 보냈는지" 알려주기 위해)
        String senderUsername = userService.getUsername(senderUserId).orElse("unknown");

        // 4) MessageService에 "저장 + 대상 선별 + 전송 요청"을 일괄 위임
        //어떤 사용자(senderUserId)가 어떤 메시지(content)를 어느 채널(channelId)에 보낼건지
        messageService.sendMessage(senderUserId, content, channelId,
                // ====== 아래가 실제 전송(I/O) 로직 ======
                (participantId) ->
                {
                    // (a) 상대방(채널 참여자)의 웹소켓 세션 찾기
                    WebSocketSession participantSession = webSocketSessionManager.getSession(participantId);

                    // (b) 전달할 알림 payload 구성
                    //     - 어느 채널(channelId)에
                    //     - 누가(senderUsername)가
                    //     - 어떤 내용을(content) 보냈는지
                    MessageNotification messageNotification = new MessageNotification(channelId, senderUsername, content);

                    //채널의 참여자 세션이 null이 아니라면, 채널의 참여자들에게 실시간 채팅 전송
                    if (participantSession != null) {
                        webSocketSessionManager.sendMessage(participantSession, messageNotification);
                    }
                });
    }


}

