package com.chatting.messageclient.dto.websocket.inbound;

import com.chatting.messageclient.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** [AcceptResponse (MessageType.ACCEPT_RESPONSE)]
 *
 * 언제: 수락을 요청한 사람(B)이 서버에 AcceptRequest를 보냈을 때, 서버가 그 요청을 요청자(B) 에게 회신.
 * 누가 받나: 수락을 보낸 사람(B).
 * 필드: username — (보통) 수락에 관련된 상대방(초대한 사람 A)의 username 혹은 확인용 정보.
 * 클라이언트 처리: 수락자(B)는 로컬에 성공 여부 반영(“수락 완료”), UI 갱신.
 */
public class AcceptResponse extends BaseMessage {

    //수락에 관련된 상대방(초대한 사람)의 username
    //초대 요청함 > 수락함 > 초대요청자에게 수락했다고 알려줘야 함
    private final String username;

    @JsonCreator
    public AcceptResponse(@JsonProperty("username") String username) {
        super(MessageType.ACCEPT_RESPONSE);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}