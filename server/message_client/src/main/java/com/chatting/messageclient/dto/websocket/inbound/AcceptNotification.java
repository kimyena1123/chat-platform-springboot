package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [AcceptNotification (MessageType.NOTIFY_ACCEPT)]
 *
 * 언제: B(초대한 사람)를 A(초대받은 사람)가 수락(AcceptRequest)했을 때 서버가 초대한 사람(A) 에게 알려준다.
 * 누가 받나: 초대한 사람(A).
 * 필드: username — 수락한 사람(B)의 username.
 * 클라이언트 처리: 알림 표시(“B가 당신의 초대를 수락했습니다”), 연결 목록 업데이트(ACCEPTED로 변경), 채팅창 오픈 가능.
 */

public class AcceptNotification extends BaseMessage {

    //수락한 사람의 username.
    private final String username;

    @JsonCreator
    public AcceptNotification(@JsonProperty("username") String username) {
        super(MessageType.NOTIFY_ACCEPT);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
