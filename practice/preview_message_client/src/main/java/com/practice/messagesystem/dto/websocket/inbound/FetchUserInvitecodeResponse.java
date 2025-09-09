package com.practice.messagesystem.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.practice.messagesystem.contants.MessageType;
import com.practice.messagesystem.dto.domain.InviteCode;

/** [FetchUserInvitecodeResponse]
 *
 * 언제: 클라이언트가 자신의 초대코드 조회 요청을 보냈을 때 서버가 회신.
 * 누가 받나: 요청자(자기 자신).
 * 필드: inviteCode — InviteCode (string).
 * 클라이언트 처리: 초대코드 표시(공유 버튼 등).
 * 예시 JSON:
 */
public class FetchUserInvitecodeResponse extends BaseMessage {

    private final InviteCode inviteCode;

    @JsonCreator
    public FetchUserInvitecodeResponse(@JsonProperty("inviteCode") InviteCode inviteCode) {
        super(MessageType.FETCH_USER_INVITECODE_RESPONSE);
        this.inviteCode = inviteCode;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }
}
