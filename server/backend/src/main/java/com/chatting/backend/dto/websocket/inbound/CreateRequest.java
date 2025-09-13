package com.chatting.backend.dto.websocket.inbound;


import com.chatting.backend.constant.MessageType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CreateRequest extends BaseRequest{

    //처음에 채널 만들 때 참여시킬 상대방 이름
    private final String title;                 // 채널명
    private final String participantUsername;   // 최초 참여시킬 상대방 username

    @JsonCreator
    public CreateRequest(@JsonProperty("title") String title, @JsonProperty("participantUsername") String participantUsername){
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
