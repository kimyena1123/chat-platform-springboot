package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [ErrorResponse (MessageType.ERROR)]
 *
 * 언제: 요청 처리 도중 에러가 발생했을 때 서버가 보낸다. (요청자에게 보통 직접 보냄; 때때로 모니터링용으로 다른 쪽에도 보낼 수 있음)
 * 누가 받나: 오류를 처리해야 하는 대상(일반적으로 요청자).
 * 필드: messageType — 어떤 요청/메시지 처리 중 에러인지 식별, message — 에러 설명.
 * 클라이언트 처리: 사용자에게 오류 표시, 재시도 로직, 디버깅 로그 저장 등.
 */
public class ErrorResponse extends BaseMessage {

    private final String messageType;
    private final String message;

    @JsonCreator
    public ErrorResponse(
            @JsonProperty("messageType") String messageType, @JsonProperty("message") String message) {
        super(MessageType.ERROR);
        this.messageType = messageType;
        this.message = message;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }
}