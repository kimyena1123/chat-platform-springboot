package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [MessageNotification (MessageType.NOTIFY_MESSAGE)]
 *
 * 언제: 누군가 메시지를 전송(WriteMessageRequest)했을 때, 서버가 수신 대상자들(대화방의 다른 사용자들)에게 브로드캐스트.
 * 누가 받나: 채팅의 다른 사용자들(보낸 사람 제외 혹은 포함하여 설계대로).
 * 필드: username (보낸 사람), content (메시지 본문).
 * 클라이언트 처리: 채팅 윈도우 업데이트, 알림 소리, 메시지 저장(옵션).
 */
public class MessageNotification extends BaseMessage {

    private final String username;  //보낸 사람의 username
    private final String content;   //메시지 본문

    @JsonCreator
    public MessageNotification(
            @JsonProperty("username") String username, @JsonProperty("content") String content) {
        super(MessageType.NOTIFY_MESSAGE);
        this.username = username;
        this.content = content;
    }

    public String getUsername() {
        return username;
    }

    public String getContent() {
        return content;
    }
}