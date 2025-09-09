package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.chatting.messageclient.constant.UserConnectionStatus;
import com.chatting.messageclient.dto.domain.InviteCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [InviteResponse (MessageType.INVITE_RESPONSE)]
 *
 * 언제: 클라이언트(A)가 InviteRequest를 보냈을 때 그 요청에 대한 서버의 직접 응답(성공/실패/현재 상태).
 * 누가 받나: 초대 요청을 보낸 사람(A).
 * 필드:
 * inviteCode — (선택적) 초대 대상의 초대코드(요청 성공 시 필요하면 포함)
 * status — 현재 관계 상태 (NONE/PENDING/ACCEPTED/REJECTED/DISCONNECTED 등)
 * 클라이언트 처리: 초대 버튼 클릭 후 받은 응답을 UI에 반영(성공이면 “초대 보냄”, 이미 연결이면 “이미 연결됨” 메시지 등).
 */
public class InviteResponse extends BaseMessage {

    private final InviteCode inviteCode;        //초대 대상의 초대코드(요청 성공 시 필요하면 포함)
    private final UserConnectionStatus status;  //현재 관계 상태 (NONE/PENDING/ACCEPTED/REJECTED/DISCONNECTED 등)

    @JsonCreator
    public InviteResponse(
            @JsonProperty("inviteCode") InviteCode inviteCode,
            @JsonProperty("status") UserConnectionStatus status) {
        super(MessageType.INVITE_RESPONSE);
        this.inviteCode = inviteCode;
        this.status = status;
    }

    public InviteCode getInviteCode() {
        return inviteCode;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}
