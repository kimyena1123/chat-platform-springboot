package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 서버 -> 클라이언트(=클라이언트가 받아들이는) 메시지들
 * inbound: 클라이언트(브라우저) 관점에서 수신(inbound)하는 메시지들(=서버가 보내는 메시지)를 모아둔 것임
 *
 * BaseMessage: 서버가 클라이언트로 다양한 형태의 메시지(InviteResponse, InviteNotification, MessageNotification 등)를 보낸다.
 *              각기 다른 구조를 가진 JSON을 하나의 수신 엔드포인트에서 처리하려면 JSON에 type 필드를 넣고, Jackson이 그 값에 따라 적절한 클래스(서브타입)로 역직렬화하도록 해야 한다.
 *
 *  예: {"type":"ASK_INVITE", "username":"alice"} → Jackson은 type이 ASK_INVITE이므로 InviteNotification으로 역직렬화해야 한다.
 *
 *  각 서브클래스는 @JsonCreator + @JsonProperty 로 필드 매핑이 되어 있어야 역직렬화가 정확하게 된다.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InviteResponse.class, name = MessageType.INVITE_RESPONSE),
        @JsonSubTypes.Type(value = AcceptResponse.class, name = MessageType.ACCEPT_RESPONSE),
        @JsonSubTypes.Type(value = RejectResponse.class, name = MessageType.REJECT_RESPONSE),
        @JsonSubTypes.Type(value = DisconnectResponse.class, name = MessageType.DISCONNECT_RESPONSE),
        @JsonSubTypes.Type(value = CreateResponse.class, name = MessageType.CREATE_RESPONSE),
        @JsonSubTypes.Type(value = EnterResponse.class, name = MessageType.ENTER_RESPONSE),
        @JsonSubTypes.Type(value = FetchConnectionsResponse.class, name = MessageType.FETCH_CONNECTIONS_RESPONSE),
        @JsonSubTypes.Type(value = FetchUserInvitecodeResponse.class, name = MessageType.FETCH_USER_INVITECODE_RESPONSE),

        @JsonSubTypes.Type(value = InviteNotification.class, name = MessageType.ASK_INVITE),
        @JsonSubTypes.Type(value = JoinNotification.class, name = MessageType.NOTIFY_JOIN),
        @JsonSubTypes.Type(value = AcceptNotification.class, name = MessageType.NOTIFY_ACCEPT),
        @JsonSubTypes.Type(value = MessageNotification.class, name = MessageType.NOTIFY_MESSAGE),
        @JsonSubTypes.Type(value = ErrorResponse.class, name = MessageType.ERROR)
})
public abstract class BaseMessage {

    private final String type;

    public BaseMessage(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}