package com.chatting.backend.constant;

/**
 * 서버와 클라이언트가 서로 주고받는 메시지의 종류를 문자열 상수로 정의해 놓은 클래스
 * 기능: 채팅 초대, 패팅 초대 승인, 채팅 메시지 전송
 */
public class MessageType {

    // client → server 방향 메시지
    public static final String INVITE_REQUEST = "INVITE_REQUEST";   // 초대 요청
    public static final String INVITE_RESPONSE = "INVITE_RESPONSE"; // 초대 요청에 대한 응답
    public static final String ACCEPT_REQUEST = "ACCEPT_REQUEST";   // 초대 승인
    public static final String ACCEPT_RESPONSE = "ACCEPT_RESPONSE"; // 초대 승읜에 대한 응답
    public static final String WRITE_MESSAGE = "WRITE_MESSAGE";     // 채팅 메시지 전송

    // server → client 방향 Notification (알림)
    public static final String ASK_INVITE = "ASK_INVITE";           // "당신이 초대를 받았습니다" 라는 알림
    public static final String NOTIFY_ACCEPT = "NOTIFY_ACCEPT";     // 초대 승인이 됐다는 알림
    public static final String NOTIFY_MESSAGE = "NOTIFY_MESSAGE";   // 새로운 채팅 메시지가 도착했다는 알림
    public static final String KEEP_ALIVE = "KEEP_ALIVE";           //
    public static final String ERROR = "ERROR";                     // 에러 발생 알림


    // ─────────────────────────────────────────────────────────────────────────────
    // [초대 흐름: INVITE]
    // ─────────────────────────────────────────────────────────────────────────────
    /**
     * 초대 요청
     * - 상황: 사용자가 상대방의 초대코드(inviteCode)를 입력해서 초대를 보낼 때.
     * - 서버 처리: 초대 가능 여부를 판단하고 상태를 PENDING으로 저장한 뒤,
     *             (1) 초대한 본인에게 "INVITE_RESPONSE",
     *             (2) 초대받은 상대에게 "ASK_INVITE" 를 전송.
     *
     * - 예시(JSON):
     *   {
     *     "type": "INVITE_REQUEST",
     *     "userInviteCode": "abcd1234"   // 상대방의 초대코드
     *   }
     */

    // ─────────────────────────────────────────────────────────────────────────────
    // [수락 흐름: ACCEPT]
    // ─────────────────────────────────────────────────────────────────────────────
    /**
     * [초대 수락 요청]
     * - 상황: 초대받은 사용자가 초대를 수락할 때 보냄.
     * - 서버 처리: 관계 상태를 ACCEPTED로 갱신하고,
     *             (1) 수락한 본인에게 "ACCEPT_RESPONSE",
     *             (2) 초대한 상대에게 "NOTIFY_ACCEPT" 를 전송.
     *
     * - 예시(JSON):
     *   {
     *     "type": "ACCEPT_REQUEST",
     *     "inviterUserId": 1          // (예) 나를 초대한 유저 ID
     *   }
     */

    // ─────────────────────────────────────────────────────────────────────────────
    // [메시지 전송 흐름]
    // ─────────────────────────────────────────────────────────────────────────────
    /**
     * 채팅 메시지 전송 요청
     * - 상황: 사용자가 채팅 입력 후 전송 버튼을 눌렀을 때.
     * - 서버 처리: 영속화(DB 저장) 및 수신 대상들에게 NOTIFY_MESSAGE로 브로드캐스트.
     *
     * - 예시(JSON):
     *   {
     *     "type": "WRITE_MESSAGE",
     *     "username": "userA",
     *     "content": "안녕!"
     *   }
     */

}