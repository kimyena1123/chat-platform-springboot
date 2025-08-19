package com.chatting.backend.constant;

/**
 * 서버와 클라이언트가 서로 주고받는 메시지의 종류를 문자열 상수로 정의해 놓은 클래스
 */
public class MessageType {

    // client → server 방향 메시지
    public static final String INVITE_REQUEST = "INVITE_REQUEST";   // 초대 요청
    public static final String INVITE_RESPONSE = "INVITE_RESPONSE"; // 초대 요청에 대한 응답
    public static final String WRITE_MESSAGE = "WRITE_MESSAGE";     // 채팅 메시지 전송

    // server → client 방향 Notification (알림)
    public static final String ASK_INVITE = "ASK_INVITE";           // "당신이 초대를 받았습니다" 라는 알림
    public static final String NOTIFY_MESSAGE = "NOTIFY_MESSAGE";   // 새로운 채팅 메시지가 도착했다는 알림
    public static final String KEEP_ALIVE = "KEEP_ALIVE";           //
    public static final String ERROR = "ERROR";                     // 에러 발생 알림


    // INVITE_REQUEST를 받으면 서버는 두 개의 메시지를 보냄
    //   1) INVITE_RESPONSE → 요청을 보낸 클라이언트에게 응답
    //   2) ASK_INVITE → 초대를 받은 대상에게 알림

}