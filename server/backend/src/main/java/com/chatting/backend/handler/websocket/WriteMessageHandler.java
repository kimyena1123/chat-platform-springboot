package com.chatting.backend.handler.websocket;

import com.chatting.backend.dto.websocket.inbound.WriteMessage;
import com.chatting.backend.dto.websocket.outbound.MessageNotification;
import com.chatting.backend.entity.MessageEntity;
import com.chatting.backend.repository.MessageRepository;
import com.chatting.backend.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 * 이 MessageHandler가 inbound로 들어오는 것마다 하나씩 대등하도록 할 것이다.
 */
@Component
@RequiredArgsConstructor
public class WriteMessageHandler implements BaseRequestHandler<WriteMessage> {

    private final WebSocketSessionManager webSocketSessionManager;
    private final MessageRepository messageRepository;

    /** [상대방에게 메시지를 전달하는 역할 수행]
     *
     * @param senderSession sender의 Session(메시지를 보내는 사람의 세션)
     * @param request sender가 보내는 메시지(메시지 내용)
     */
    @Override
    public void handleRequest(WebSocketSession senderSession, WriteMessage request) {
        //senderSession(채팅 보내는 사람)이 자신의 username과 자신이 보낼 메시지의 content를 담아서 receivedMessage에 담는다.
        // Message에는 username과 content가 있다
        MessageNotification receivedMessage = new MessageNotification(
                request.getUsername(), // 보낸 사람의 username
                request.getContent()   // 보낸 사람이 입력한 메시지 내용
        );

        //DB의 message 테이블에 메시지 보낸 당사자의 username과 메시지 content를 저장한다(넣는다)
        messageRepository.save(new MessageEntity(receivedMessage.getUsername(), receivedMessage.getContent()));

        //해당 채팅방에 들어있는 모든 사람들에게 내가 작성한 메시지 전송하기(1:1일수도, 그룹일수도 있음)
        webSocketSessionManager
                .getSessions() //현재 채팅방에 있는 모든 session 가져오기(채팅 참여자들의 session 리스트 가져오기)
                .forEach( // 가져온 session 리스트를 하나씩 돌면서
                        participantSession -> { // 채팅방의 각 참여자들에게(채팅방에는 메시지를 보내는 자기 자신도 포함되어 있음. 모든 세션을 가져왔기 때문에!)
                            if(!senderSession.getId().equals(participantSession.getId())){ // 자기자신을 제외한 나머지 채팅 참여자들에게 메시지 전송
                                webSocketSessionManager.sendMessage(participantSession, receivedMessage); //sendMessage(채팅받는 사람, 보낼 메시지)
                            }
                        }
                );
    }
}
