package com.chatting.push_server.dto.kafka.inbound;

import com.chatting.push_server.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * [다형(Polymorphic) JSON 역직렬화를 위한 인터페이스]
 *
 * - 카프카 value(JSON) 안에 "type" 필드를 넣고,
 *   그 값에 따라 서로 다른 레코드(서브타입)로 역직렬화하기 위한 설정.
 *
 * 예)
 * {"type":"ACCEPT_RESPONSE", "userId":123, "username":"alice" }
 *  → Jackson이 AcceptResponseRecord로 자동 역직렬화.
 *
 * 핵심 어노테이션:
 * - @JsonTypeInfo: JSON에 담긴 "type" 속성을 보고 서브타입을 고르게 한다.
 * - @JsonSubTypes: type 값 ↔ 서브타입 클래스 매핑 목록.
 *
 * 장점:
 * - Consumer(진입점) 코드는 JSON 필드를 일일이 파싱/분기할 필요가 없다.
 * - 새로운 메시지 타입이 추가되어도, 여기 매핑과 해당 레코드/핸들러만 추가하면 확장 가능.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = InviteResponseRecord.class, name = MessageType.INVITE_RESPONSE),
        @JsonSubTypes.Type(value = AcceptResponseRecord.class, name = MessageType.ACCEPT_RESPONSE),
        @JsonSubTypes.Type(value = RejectResponseRecord.class, name = MessageType.REJECT_RESPONSE),
        @JsonSubTypes.Type(value = DisconnectResponseRecord.class, name = MessageType.DISCONNECT_RESPONSE),
        @JsonSubTypes.Type(value = CreateResponseRecord.class, name = MessageType.CREATE_RESPONSE),
        @JsonSubTypes.Type(value = QuitResponseRecord.class, name = MessageType.QUIT_RESPONSE),

        @JsonSubTypes.Type(value = InviteNotificationRecord.class, name = MessageType.ASK_INVITE),
        @JsonSubTypes.Type(value = JoinNotificationRecord.class, name = MessageType.NOTIFY_JOIN),
        @JsonSubTypes.Type(value = AcceptNotificationRecord.class, name = MessageType.NOTIFY_ACCEPT),
        @JsonSubTypes.Type(value = MessageNotificationRecord.class, name = MessageType.NOTIFY_MESSAGE),
})

public interface RecordInterface {

    String type(); // 서브타입마다 자신이 표현하는 "type"을 돌려주도록 표준화(로깅/검증 등에 사용 가능)
}