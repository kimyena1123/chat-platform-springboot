package com.chatting.messageclient.dto.websocket.outbound;


import com.chatting.messageclient.constant.MessageType;

import java.util.List;

public class CreateRequest extends BaseRequest {

    //처음에 채널 만들 때 참여시킬 상대방 이름
    private final String title;                 // 채널명
    private final List<String> participantUsernames;     // 단톡방 기능도 추가되어 List

    public CreateRequest(String title, List<String> participantUsernames) {
        super(MessageType.CREATE_REQUEST);

        this.title = title;
        this.participantUsernames = participantUsernames;
    }

    //Getter
    public String getTitle() {
        return title;
    }

    public List<String> getParticipantUsernames() {
        return participantUsernames;
    }
}
