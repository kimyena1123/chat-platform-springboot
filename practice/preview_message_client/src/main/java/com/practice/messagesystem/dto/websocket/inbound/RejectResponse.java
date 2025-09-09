package com.practice.messagesystem.dto.websocket.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.practice.messagesystem.contants.MessageType;
import com.practice.messagesystem.contants.UserConnectionStatus;

/** [RejectResponse (MessageType.REJECT_RESPONSE)
 *
 * 언제: 누군가의 초대가 거절되었을 때, 서버가 초대한 사람(A) 에게 알려주기 위해 보낼 수 있음(프로젝트 정책에 따라서는 거절자(B)에게도 확인 응답을 보내기도 함).
 * 누가 받나: 보통 초대를 보낸 사람(A) — “당신의 초대가 거절되었음”.
 * 필드: username(누가 거절했는지), status (REJECTED 등).
 * 클라이언트 처리: 알림 표시(“B가 당신의 초대를 거절했습니다”), 필요 시 UI에서 PENDING 제거 등.
 */
public class RejectResponse extends BaseMessage {

    private final String username;              //거절하는 사람의 username(누가 거절했는지)
    private final UserConnectionStatus status;

    @JsonCreator
    public RejectResponse(
            @JsonProperty("username") String username,
            @JsonProperty("status") UserConnectionStatus status) {
        super(MessageType.REJECT_RESPONSE);
        this.username = username;
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public UserConnectionStatus getStatus() {
        return status;
    }
}