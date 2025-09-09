package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [InviteNotification (MessageType.ASK_INVITE)]
 *
 * 언제: 누군가(A)가 다른 사람(B)의 초대코드로 초대(InviteRequest)를 보냈을 때, 서버가 초대받는 쪽(B) 에게 알려줄 때.
 * 누가 받나: 초대받는 사용자(B).
 * 필드: username — 초대를 보낸 사람(A)의 username.
 * 클라이언트 처리: 알림 UI 표시(「A가 당신을 초대했습니다」), 알림 클릭 시 수락/거절 UI로 이동, 로컬 PENDING 목록 업데이트 등.
 */
public class InviteNotification extends BaseMessage {

    //목적: 누가 나를 초대했다.
    //초대를 보낸 사람의 username(초대한 사람의 username)
    private final String username;

    @JsonCreator
    public InviteNotification(@JsonProperty("username") String username) {
        super(MessageType.ASK_INVITE);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
