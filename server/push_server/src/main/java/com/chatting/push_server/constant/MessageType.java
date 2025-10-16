package com.chatting.push_server.constant;

/**
 * [메시지 타입 상수]
 *
 * - JSON의 "type" 필드 표준화.
 * - RecordInterface의 @JsonSubTypes와 1:1로 대응하여
 *   역직렬화 시 "어떤 서브타입으로 바꿀지"를 결정하는 키가 된다.
 */

public class MessageType {

    public static final String INVITE_RESPONSE = "INVITE_RESPONSE";
    public static final String ACCEPT_RESPONSE = "ACCEPT_RESPONSE";
    public static final String REJECT_RESPONSE = "REJECT_RESPONSE";
    public static final String DISCONNECT_RESPONSE = "DISCONNECT_RESPONSE";
    public static final String CREATE_RESPONSE = "CREATE_RESPONSE";
    public static final String LEAVE_RESPONSE = "LEAVE_RESPONSE";
    public static final String QUIT_RESPONSE = "QUIT_RESPONSE";

    public static final String ASK_INVITE = "ASK_INVITE";
    public static final String NOTIFY_ACCEPT ="NOTIFY_ACCEPT";
    public static final String NOTIFY_JOIN = "NOTIFY_JOIN";
    public static final String NOTIFY_MESSAGE = "NOTIFY_MESSAGE";
}