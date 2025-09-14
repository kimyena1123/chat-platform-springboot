package com.chatting.messageclient.dto.websocket.outbound;


import com.chatting.messageclient.constant.MessageType;

public class CreateRequest extends BaseRequest {

    //처음에 채널 만들 때 참여시킬 상대방 이름
    private final String title;                 // 채널명
    private final String participantUsername;   // 최초 참여시킬 상대방 username

    public CreateRequest(String title, String participantUsername) {
        super(MessageType.CREATE_REQUEST);

        this.title = title;
        this.participantUsername = participantUsername;
    }

    //Getter
    public String getTitle() {
        return title;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }
}
