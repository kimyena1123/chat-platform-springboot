package com.chatting.messageclient.constant;

/** [서버와 클라이언트가 서로 주고받는 메시지의 종류를 문자열 상수로 정의해 놓은 클래스]
 *
 * HTTP 기반 REST API에서는
 * 클라이언트 -> 서버 = request
 * 서버 -> 클라이언트 = response 구조가 명확하다.
 *
 * 하지만 지금 구현하는 건 WebSocket 기반의 양방향 통신이다.
 * 여기서는 "요청-응답"의 개념뿐만 아니라, 서버가 스스로 알림을 푸시(push) 할 수 있는 구조가 가능해진다.
 *
 * 1. request(요청)       : 클라이언트가 서버에게 "이거 해줘"라고 명시적으로 보내는 것.
 * 2. response(응답)      : 서버가 해당 요청에 대해 결과를 알려주는 것
 * 3. Notification(알림)  : 클라이언트가 요청하지 않았어도, 서버가 "이런 이벤트가 발생했어"라고 알려주는 것
 *      - ex. 누군가 나를 초대했거나, 메시지를 보냈거나, 서버가 연결이 살아았는지 확인하는 신호 (이것은 요청/응답과 독립적으로 발생할 수 있다)
 */
public class MessageType {

    //서버가 메시지를 보내줄 때는 NOTIFY_MESSAGE로 보내주고
    //클라이언트가 메시지를 쓸 때는 WRITE_MESSAGE를 쓴다.

    //===============================================================
    //=========================== 요청, 응답 ==========================
    //===============================================================

    public static final String FETCH_USER_INVITECODE_REQUEST = "FETCH_USER_INVITECODE_REQUEST";     //자기자신의 초대코드를 받기 위한 요청
    public static final String FETCH_USER_INVITECODE_RESPONSE = "FETCH_USER_INVITE_CODE_RESPONSE";  //자기자신의 초대코드를 요청한 것에 대한 응답

    public static final String FETCH_CONNECTIONS_REQUEST = "FETCH_CONNECTIONS_REQUEST";     //나의 연결상태 목록을 보기 위한 요청(ex. 나와 ACCEPTED 상태인 사람들의 목록을 보고싶다)
    public static final String FETCH_CONNECTIONS_RESPONSE = "FETCH_CONNECTIONS_RESPONSE";   //나의 연결상태 목록 요청한 것에 대한 응답

    public static final String INVITE_REQUEST = "INVITE_REQUEST";           //채팅 초대 요청
    public static final String INVITE_RESPONSE = "INVITE_RESPONSE";         //채팅 초대 요청한 것에 대한 응답

    public static final String ACCEPT_REQUEST = "ACCEPT_REQUEST";           //채팅 초대 요청 수락
    public static final String ACCEPT_RESPONSE = "ACCEPT_RESPONSE";         //요청 수락을 한 것에 대한 응답

    public static final String REJECT_REQUEST = "REJECT_REQUEST";           //채팅 초대 요청 거절
    public static final String REJECT_RESPONSE = "REJECT_RESPONSE";         //요청 거절을 한 것에 대한 응답(reject은 요청한 사람한테만 응답을 주면 된다. 상대방한테 너 거절당했어 라는 응답을 줄 필요 없다)

    public static final String DISCONNECT_REQUEST = "DISCONNECT_REQUEST";   //채팅 연결 끊기
    public static final String DISCONNECT_RESPONSE = "DISCONNECT_RESPONSE"; //연결을 끊은 것에 대한 응답(요청한 사람한테만 응답을 주면 된다. 상대방한테 너 연결 끊겼다 라는 응답을 줄 필요 없다)

    public static final String WRITE_MESSAGE = "WRITE_MESSAGE";             //채팅 메시지 전송


    //===============================================================
    //============ server → client 방향 Notification (알림) ===========
    //===============================================================

    public static final String ASK_INVITE = "ASK_INVITE";               //"누군가 당신을 초대했어요"라는 알림
    public static final String NOTIFY_ACCEPT = "NOTIFY_ACCEPT";         //"당신의 초대가 수락됐습니다"라는 알림
    public static final String NOTIFY_MESSAGE = "NOTIFY_MESSAGE";       //"새로운 채팅 메시지가 도착했습니다"라는 알림
    public static final String KEEP_ALIVE = "KEEP_ALIVE";               //"서버와 연결이 여전히 유지되고 있음을 확인"하는 알림
    public static final String ERROR = "ERROR";                         //"요청 처리 중 문제가 생겼음"을 알려주는 알림


}